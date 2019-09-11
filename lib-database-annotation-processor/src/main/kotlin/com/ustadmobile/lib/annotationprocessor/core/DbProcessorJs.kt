package com.ustadmobile.lib.annotationprocessor.core

import androidx.room.Dao
import androidx.room.Database
import com.squareup.kotlinpoet.*
import io.ktor.client.HttpClient
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import com.ustadmobile.door.DatabaseBuilder
import io.ktor.client.features.json.JsonFeature

private fun TypeSpec.Builder.addDbJsImplPropsAndConstructor(addHttpClient: Boolean = true,
                                                            addJsonProp: Boolean = true): TypeSpec.Builder {
    addProperty(PropertySpec.builder("_endpoint", String::class)
            .initializer("_endpoint").build())
    addProperty(PropertySpec.builder("_dbPath", String::class)
            .initializer("_dbPath").build())
    val constructorSpec = FunSpec.constructorBuilder()

    if(addHttpClient) {
        addProperty(PropertySpec.builder("_httpClient", HttpClient::class)
                .initializer("_httpClient").build())
        constructorSpec.addParameter("_httpClient", HttpClient::class)
    }

    constructorSpec.addParameter("_endpoint", String::class)
                    .addParameter("_dbPath", String::class)

    if(addJsonProp) {
        addProperty(PropertySpec.builder("_json", DbProcessorJs.KOTLINX_JSON_CLASSNAME)
                .initializer("_json")
                .build())
        constructorSpec.addParameter("_json", DbProcessorJs.KOTLINX_JSON_CLASSNAME)
    }


    primaryConstructor(constructorSpec.build())

    return this
}

class DbProcessorJs : AbstractDbProcessor(){

    override fun process(elements: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        //find all databases on the source path
        if (processingEnv.options[AnnotationProcessorWrapper.OPTION_JS_OUTPUT] == null) {
            //skip output
            return true
        }

        roundEnv.getElementsAnnotatedWith(Database::class.java).map {it as TypeElement}.forEach { dbEl ->
            writeFileSpecToOutputDirs(generateDbJsImpl(dbEl),
                    AnnotationProcessorWrapper.OPTION_JS_OUTPUT, false)
        }

        roundEnv.getElementsAnnotatedWith(Dao::class.java).forEach { daoEl ->
            writeFileSpecToOutputDirs(generateDaoRepositoryClass(daoEl as TypeElement),
                    AnnotationProcessorWrapper.OPTION_JS_OUTPUT, false)
        }


        return true
    }


    fun generateDbJsImpl(dbTypeEl: TypeElement) : FileSpec {
        val dbTypeClassName = dbTypeEl.asClassName()
        val dbType = dbTypeEl.asType() as DeclaredType
        val implFileSpec = FileSpec.builder(dbTypeClassName.packageName,
                "${dbTypeClassName.simpleName}$SUFFIX_JS_IMPL")

        val implTypeSpec = TypeSpec.classBuilder(implFileSpec.name)
                .addDbJsImplPropsAndConstructor(addHttpClient = false, addJsonProp = false)
                .superclass(dbTypeEl.asClassName())
                .addFunction(FunSpec.builder("clearAllTables")
                        .addModifiers(KModifier.OVERRIDE)
                        .build())
                .addProperty(PropertySpec.builder("master", BOOLEAN)
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer("false").build())
                .addType(TypeSpec.companionObjectBuilder()
                        .addFunction(FunSpec.builder("register")
                                .addCode("%T.registerImpl(%T::class, %T::class)\n",
                                        DatabaseBuilder::class, dbTypeEl,
                                        ClassName(dbTypeClassName.packageName, implFileSpec.name))
                                .build())
                        .build())

        val initSerializerCodeBlock = CodeBlock.of("%T()", KOTLINX_SERIALIZATION_CLASSNAME)

        val initHttpClientCodeBlock = CodeBlock.builder()
                .beginControlFlow("%T()", HttpClient::class)
                .beginControlFlow("install(%T)", JsonFeature::class)
                .add("serializer = _serializer\n")
                .endControlFlow()
                .endControlFlow()


        implTypeSpec.addProperty(PropertySpec.builder("_serializer", KOTLINX_SERIALIZATION_CLASSNAME)
                    .initializer(initSerializerCodeBlock.build()).build())
                .addProperty(PropertySpec.builder("_httpClient", HttpClient::class)
                    .initializer(initHttpClientCodeBlock.build())
                .build())
                .addProperty(PropertySpec.builder("_json", KOTLINX_JSON_CLASSNAME)
                        .initializer(CodeBlock.of("%T(%T.Stable)", KOTLINX_JSON_CLASSNAME,
                                ClassName("kotlinx.serialization.json", "JsonConfiguration")))
                        .build())

        val daoGetterMethods = methodsToImplement(dbTypeEl, dbType, processingEnv)
                .filter{it.kind == ElementKind.METHOD }.map {it as ExecutableElement }

        daoGetterMethods.forEach {
            val daoTypeEl = processingEnv.typeUtils.asElement(it.returnType) as TypeElement?
            if(daoTypeEl == null)
                return@forEach

            val daoTypeClassName = daoTypeEl.asClassName()

            val daoJsImplClassName = ClassName(daoTypeClassName.packageName,
                    "${daoTypeClassName.simpleName}$SUFFIX_JS_IMPL")

            implTypeSpec.addProperty(PropertySpec.builder("_${daoTypeClassName.simpleName}",
                    daoTypeClassName)
                    .delegate("lazy { %T(_httpClient, _endpoint, _dbPath, _json) }", daoJsImplClassName)
                    .build())

            implTypeSpec.addAccessorOverride(it, CodeBlock.of("return _${daoTypeClassName.simpleName}\n"))
        }

        return implFileSpec.addType(implTypeSpec.build()).build()
    }


    fun generateDaoRepositoryClass(daoTypeEl: TypeElement): FileSpec {
        val daoTypeClassName = daoTypeEl.asClassName()
        val daoType = daoTypeEl.asType()
        val daoImplFile = FileSpec.builder(daoTypeClassName.packageName,
                "${daoTypeEl.simpleName}$SUFFIX_JS_IMPL")

        val daoTypeSpec = TypeSpec.classBuilder("${daoTypeClassName.simpleName}$SUFFIX_JS_IMPL")
                .addDbJsImplPropsAndConstructor()
                .superclass(daoTypeEl.asClassName())

        methodsToImplement(daoTypeEl, daoType as DeclaredType, processingEnv).forEach {daoSubEl ->
            if (daoSubEl.kind != ElementKind.METHOD)
                return@forEach

            val daoMethodEl = daoSubEl as ExecutableElement

            val daoMethodResolved = processingEnv.typeUtils.asMemberOf(daoType, daoMethodEl) as ExecutableType

            val returnTypeResolved = resolveReturnTypeIfSuspended(daoMethodResolved).javaToKotlinType()
            val resultType = resolveQueryResultType(returnTypeResolved)

            //daoFunSpec is generated so that it can be passed to the generateKtorRequestCodeBlock method
            val (overrideFunSpec, daoFunSpec) = (0..1).map {overrideAndConvertToKotlinTypes(daoMethodEl,
                    daoType, processingEnv,
                    forceNullableReturn = isNullableResultType(returnTypeResolved),
                    forceNullableParameterTypeArgs = isLiveData(returnTypeResolved)
                            && isNullableResultType((returnTypeResolved as ParameterizedTypeName).typeArguments[0])) }
                    .zipWithNext()[0]

            daoFunSpec.addAnnotations(daoMethodEl.annotationMirrors.map { AnnotationSpec.get(it) })

            if(daoMethodResolved.parameterTypes.any { isContinuationParam(it.asTypeName())}) {
                var codeBlock = generateKtorRequestCodeBlockForMethod(
                        daoName = daoTypeClassName.simpleName,
                        dbPathVarName = "_dbPath",
                        methodName = daoSubEl.simpleName.toString(),
                        httpResultType = resultType,
                        params = daoFunSpec.parameters,
                        useKotlinxListSerialization = true,
                        kotlinxSerializationJsonVarName = "_json")

                if(returnTypeResolved != UNIT) {
                    codeBlock = CodeBlock.builder().add(codeBlock).add("return _httpResult\n").build()
                }

                overrideFunSpec.addCode(codeBlock)
            }else {
                overrideFunSpec.addCode("throw %T(%S)\n", ClassName("kotlin", "IllegalStateException"),
                        "Javascript can only access DAO functions which are suspended or return LiveData/DataSource")
            }

            daoTypeSpec.addFunction(overrideFunSpec.build())
        }

        daoImplFile.addType(daoTypeSpec.build())
        return daoImplFile.build()

    }

    companion object {
        const val SUFFIX_JS_IMPL = "_JsImpl"

        val KOTLINX_SERIALIZATION_CLASSNAME = ClassName("io.ktor.client.features.json.serializer",
                "KotlinxSerializer")

        val KOTLINX_JSON_CLASSNAME = ClassName("kotlinx.serialization.json", "Json")

    }


}
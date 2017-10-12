/*
    This file is part of Ustad Mobile.

    Ustad Mobile Copyright (C) 2011-2014 UstadMobile Inc.

    Ustad Mobile is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version with the following additional terms:

    All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
    LLC must be kept as they are in the original distribution.  If any new
    screens are added you must include the Ustad Mobile logo as it has been
    used in the original distribution.  You may not create any new
    functionality whose purpose is to diminish or remove the Ustad Mobile
    Logo.  You must leave the Ustad Mobile logo as the logo for the
    application to be used with any launcher (e.g. the mobile app launcher).

    If you want a commercial license to remove the above restriction you must
    contact us.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Ustad Mobile is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

 */


package com.ustadmobile.port.android.view;


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.CatalogPresenter;
import com.ustadmobile.core.controller.ControllerReadyListener;
import com.ustadmobile.core.controller.UstadController;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.model.CourseProgress;
import com.ustadmobile.core.opds.UstadJSOPDSEntry;
import com.ustadmobile.core.opds.UstadJSOPDSItem;
import com.ustadmobile.core.view.CatalogView;
import com.ustadmobile.port.android.util.UMAndroidUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * An Android Fragment that implements the CatalogView to show an OPDS Catalog
 *
 * Use newInstance to create a new Fragment and use the FragmentManager in the normal way
 *
 */
public class CatalogOPDSFragment extends UstadBaseFragment implements View.OnClickListener,
        View.OnLongClickListener, CatalogView, SwipeRefreshLayout.OnRefreshListener{

    private View rootContainer;

    private Map<String, OPDSEntryCard> idToCardMap;

    private Map<String, String> idToThumbnailUrlMap;

    private Map<String, Integer> idToStatusMap = new Hashtable<>();

    private Map<String, Boolean> idToProgressVisibleMap = new Hashtable<>();

    private UstadJSOPDSEntry[] mSelectedEntries;

    private boolean hasDisplayed = false;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    //Trackers whether or not there is a loading operation (e.g. refresh) going on
    private boolean isLoading = false;

    private boolean mDeleteOptionAvailable;

    private boolean mAddOptionAvailable;

    private static final int MENUCMDID_ADD = 1200;

    private static final int MENUCMDID_DELETE = 1201;

    private static final int MENUCMD_SHARE = 1202;

    private RecyclerView mRecyclerView;

    private RecyclerView.Adapter mRecyclerAdapter;

    private RecyclerView.LayoutManager mRecyclerLayoutManager;

    private boolean isRequesting=false;

    private String[] alternativeTranslationLanguages;

    private int alterantiveTranslationLanguagesDisabledItemIndex = -1;

    private ArrayList<MenuItem> alternativeTranslationLanguageMenuItems;

    private CatalogPresenter mCatalogPresenter;

    private ArrayList<UstadJSOPDSEntry> entryList = new ArrayList<>();


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * One can also put in the bundle frag-menuid to set the menuid to be used : otherwise menus
     * will be set according to if the feed is acquisition or navigation type
     *
     * @return A new instance of fragment CatalogOPDSFragment.
     */
    public static CatalogOPDSFragment newInstance(Bundle args) {
        CatalogOPDSFragment fragment = new CatalogOPDSFragment();
        Bundle bundle = new Bundle();
        bundle.putAll(args);
        fragment.setArguments(bundle);

        return fragment;
    }

    public CatalogOPDSFragment() {
        // Required empty public constructor
    }

    private BroadcastReceiver broadcastReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        this.rootContainer = inflater.inflate(R.layout.fragment_catalog_opds, container, false);
        mSelectedEntries = new UstadJSOPDSEntry[0];
        setHasOptionsMenu(true);

        idToCardMap = new WeakHashMap<>();
        idToThumbnailUrlMap = new HashMap<>();

        mSwipeRefreshLayout = rootContainer.findViewById(R.id.fragment_catalog_swiperefreshview);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        rootContainer.findViewById(R.id.fragment_catalog_footer_button).setOnClickListener(this);

        mRecyclerView = rootContainer.findViewById(R.id.fragment_catalog_recyclerview);
        mRecyclerLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mRecyclerLayoutManager);

        rootContainer.findViewById(R.id.fragment_catalog_addbutton).setOnClickListener(this);

        mCatalogPresenter = new CatalogPresenter(getContext(), this);

        mRecyclerAdapter = new OPDSRecyclerAdapter(entryList);
        mRecyclerView.setAdapter(mRecyclerAdapter);

        mCatalogPresenter.onCreate(UMAndroidUtil.bundleToHashtable(getArguments()),
                UMAndroidUtil.bundleToHashtable(savedInstanceState));


        return rootContainer;
    }

    @Override
    public void addEntry(int index, UstadJSOPDSEntry entry) {
        entryList.add(index, entry);
        mRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void addEntry(UstadJSOPDSEntry entry) {
        entryList.add(entry);
        mRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void setEntryAt(int position, UstadJSOPDSEntry entry) {
        entryList.set(position, entry);
        mRecyclerAdapter.notifyItemChanged(position);
    }

    @Override
    public void removeEntry(UstadJSOPDSEntry entry) {
        entryList.remove(entry);
        mRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public int indexOfEntry(String entryId) {
        return UstadJSOPDSItem.indexOfEntryInList(entryId, entryList);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
//        consider refreshing here
//        if(!hasDisplayed) {
//            hasDisplayed = true;
//        }else {
//            loadCatalog();
//        }

    }

    @Override
    public void onStop() {
        super.onStop();
        mCatalogPresenter.onStop();
    }

    /**
     * Get the OPDSEntryCard for the given OPDS Entry ID
     *
     * @param id OPDS Entry id
     * @return OPDSEntryCard representing this item
     */
    public OPDSEntryCard getEntryCardByOPDSID(String id) {
        return idToCardMap.get(id);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(mDeleteOptionAvailable && mSelectedEntries.length > 0) {
            MenuItem item = menu.add(Menu.NONE, MENUCMDID_DELETE, 1, "");
            item.setIcon(R.drawable.ic_delete_white_24dp);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        MenuItem shareItem = menu.add(Menu.NONE, MENUCMD_SHARE, 2, "");
        shareItem.setIcon(R.drawable.ic_share_white_24dp);
        shareItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if(alternativeTranslationLanguages != null) {
            SubMenu languagesSubmenu = menu.addSubMenu(Menu.NONE, 700, 3, "");
            languagesSubmenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            languagesSubmenu.getItem().setTitle(R.string.catalog_language);
            alternativeTranslationLanguageMenuItems = new ArrayList<>(alternativeTranslationLanguages.length);

            MenuItem langItem;
            for(int i = 0; i < alternativeTranslationLanguages.length; i++) {
                langItem = languagesSubmenu.add(alternativeTranslationLanguages[i]);
                if(i == alterantiveTranslationLanguagesDisabledItemIndex)
                    langItem.setEnabled(false);

                alternativeTranslationLanguageMenuItems.add(langItem);
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void setAlternativeTranslationLinks(String[] translationLinks, int disabledItem) {
        this.alternativeTranslationLanguages = translationLinks;
        alterantiveTranslationLanguagesDisabledItemIndex = disabledItem;
        if(getActivity() != null)
            getActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(alternativeTranslationLanguageMenuItems != null
                && alternativeTranslationLanguageMenuItems.contains(item)) {
            int selectedIndex = alternativeTranslationLanguageMenuItems.indexOf(item);
            mCatalogPresenter.handleClickAlternativeLanguage(selectedIndex);
        }

        switch(item.getItemId()) {
            case MENUCMDID_ADD:
                mCatalogPresenter.handleClickAdd();
                return true;
            case R.id.action_opds_acquire:
//                TODO: Handle this in the presenter not the view
//                if(getSelectedEntries().length > 0) {
////                    UstadJSOPDSFeed feed = mCatalogController.getModel().opdsFeed;
//                    UstadJSOPDSFeed feed = mCatalogController.getModel().opdsFeed;
//                    UstadJSOPDSFeed acquisitionFeed = new UstadJSOPDSFeed(feed.href, feed.title,
//                            feed.id+"-acquire");
//                    UstadJSOPDSEntry[] selectedEntries = getSelectedEntries();
//                    for(int i = 0; i  <selectedEntries.length; i++) {
//                        acquisitionFeed.addEntry(new UstadJSOPDSEntry(acquisitionFeed, selectedEntries[i]));
//                    }
//
//                    Vector selectEntriesVector = new Vector();
//                    for(int i = 0; i < selectedEntries.length; i++) {
//                        selectEntriesVector.addElement(selectedEntries[i]);
//                    }
//
//                    mCatalogController.handleClickDownload(acquisitionFeed, selectEntriesVector);
//                }else {
//                    mCatalogController.handleClickDownloadAll();
//                }

                return true;
            case MENUCMDID_DELETE:
                if(getSelectedEntries().length > 0) {
//                    TODO: Handle click delete in presenter
//                    mCatalogController.handleClickDeleteEntries(getSelectedEntries());
                }

                return true;

            case MENUCMD_SHARE:
//                mCatalogController.handleClickShare();
                return true;

        }

        if(alternativeTranslationLanguageMenuItems.contains(item)){
            //TODO: handle click alternative translation
//            mCatalogController.handleClickAlternativeTranslationLink(
//                    alternativeTranslationLanguageMenuItems.indexOf(item));
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
//        if(mCatalogController != null) {
//            mCatalogController.handleViewPause();
//        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (mCatalogController != null) {
//            mCatalogController.handleViewDestroy();
//            isRequesting=false;
//        }

//        if(mCatalogController != null)
//            mCatalogController.onDestroy();
        mCatalogPresenter.onDestroy();
    }

    public void toggleEntrySelected(OPDSEntryCard card) {
        boolean nowSelected = !card.isSelected();
        card.setSelected(nowSelected);

        //TODO: Fix this
        UstadJSOPDSEntry[] currentSelection = getSelectedEntries();
        int newArraySize = nowSelected ? currentSelection.length + 1 : currentSelection.length -1;
        UstadJSOPDSEntry[] newSelection = new UstadJSOPDSEntry[newArraySize];

        if(nowSelected) {
            //just make the new selection one larger
            System.arraycopy(currentSelection, 0, newSelection, 0, currentSelection.length);
            newSelection[newSelection.length-1] = card.getEntry();
        }else {
            for(int i = 0, elCount = 0; i < currentSelection.length; i++) {
                if(!currentSelection[i].id.equals(card.getEntry().id)) {
                    newSelection[elCount] = currentSelection[i];
                    elCount++;
                }
            }
        }

        setSelectedEntries(newSelection);
    }



    @Override
    public void onClick(View view) {
        if(view instanceof OPDSEntryCard) {
            OPDSEntryCard card = ((OPDSEntryCard)view);
            if(getSelectedEntries().length > 0) {
                toggleEntrySelected(card);
            }else {
                mCatalogPresenter.handleClickEntry(card.getEntry().id);
            }
            return;
        }

        switch(view.getId()) {
            case R.id.fragment_catalog_footer_button:
                mCatalogPresenter.handleClickFooterButton();
                break;
            case R.id.fragment_catalog_addbutton:
                mCatalogPresenter.handleClickAdd();
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if(view instanceof OPDSEntryCard) {
            OPDSEntryCard card = (OPDSEntryCard)view;
            toggleEntrySelected(card);
            return true;
        }
        return false;
    }

    @Override
    public void showConfirmDialog(String title, String message, String positiveChoice, String negativeChoice, final int commandId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message);
        builder.setTitle(title);
//        TODO: Handle the confirm dialog
        builder.setPositiveButton(positiveChoice, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id) {
//                mCatalogController.handleConfirmDialogClick(true, commandId);
            }
        });
        builder.setNegativeButton(negativeChoice, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
//                mCatalogController.handleConfirmDialogClick(false, commandId);
            }
        });
        builder.create().show();
    }


    @Override
    public void setEntryStatus(final String entryId, final int status) {
        idToStatusMap.put(entryId, status);
        super.runOnUiThread(new Runnable() {
            public void run() {
                if(idToCardMap.containsKey(entryId)) {
                    idToCardMap.get(entryId).setOPDSEntryOverlay(status);
                }
            }
        });
    }

    @Override
    public void setEntrythumbnail(final String entryId, String iconUrl) {
        idToThumbnailUrlMap.put(entryId, iconUrl);
        OPDSEntryCard card = idToCardMap.get(entryId);
        if(card != null)
            card.setThumbnailUrl(iconUrl, mCatalogPresenter, this);
    }

    @Override
    public void setEntryProgress(final String entryId, final CourseProgress progress) {
        super.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(idToCardMap != null && idToCardMap.containsKey(entryId)) {
                    idToCardMap.get(entryId).setProgress(progress);
                }
            }
        });
    }

    @Override
    public void setEntryBackground(String entryId, String backgroundFileURI) {
        //TODO: Implement setting entry background on Android
    }

    @Override
    public void setCatalogBackground(String backgroundFileURI) {
        //TODO: implement setting catalog background on Android
    }

    @Override
    public void updateDownloadAllProgress(int loaded, int total) {

    }

    @Override
    public void setDownloadEntryProgressVisible(final String entryId, final boolean visible) {
        this.idToProgressVisibleMap.put(entryId, visible);
        super.runOnUiThread(new Runnable() {
            public void run() {
                if(idToCardMap.containsKey(entryId)) {
                    idToCardMap.get(entryId).setProgressBarVisible(visible);
                }
            }
        });
    }

    @Override
    public void updateDownloadEntryProgress(final String entryId, final float progress, final String statusText) {
        super.runOnUiThread(new Runnable() {
            public void run() {
                OPDSEntryCard card = idToCardMap.get(entryId);
                if(card != null) {
                    card.setDownloadProgressBarProgress(progress);
                    card.setDownloadProgressStatusText(statusText);
                }
            }
        });
    }

    @Override
    public UstadJSOPDSEntry[] getSelectedEntries() {
        return mSelectedEntries;
    }

    @Override
    public void setSelectedEntries(UstadJSOPDSEntry[] entries) {
        if((entries.length == 0 && mSelectedEntries.length > 0) || (entries.length > 0 && mSelectedEntries.length ==0)) {
            getActivity().supportInvalidateOptionsMenu();
        }

        this.mSelectedEntries = entries;

//        UstadJSOPDSFeed thisFeed = mCatalogController.getModel().opdsFeed;
        for(int i = 0; i < entryList.size(); i++) {
            boolean isSelected = false;
            for(int j = 0; j < entries.length; j++) {

                if(entryList.get(i).id.equals(entries[j].id)) {
                    isSelected = true;
                    break;
                }
            }

            if(idToCardMap.containsKey(entryList.get(i).id)) {
                idToCardMap.get(entryList.get(i).id).setSelected(isSelected);
            }
        }

    }

    @Override
    public void refresh() {
        this.onRefresh();
    }

    /**
     * Handle when the user selects to refresh
     */
    @Override
    public void onRefresh() {
        mCatalogPresenter.handleRefresh();
    }

    @Override
    public void setRefreshing(boolean isRefreshing) {
        mSwipeRefreshLayout.setRefreshing(isRefreshing);
    }

    @Override
    public void setFooterButtonVisible(boolean buttonVisible) {
        this.rootContainer.findViewById(R.id.fragment_catalog_footer_button).setVisibility(
                buttonVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setFooterButtonLabel(String browseButtonLabel) {
        ((Button)this.rootContainer.findViewById(R.id.fragment_catalog_footer_button)).setText(browseButtonLabel);
    }

    @Override
    public void setDeleteOptionAvailable(boolean deleteOptionAvailable) {
        this.mDeleteOptionAvailable = deleteOptionAvailable;
        if(getActivity() != null)
            getActivity().invalidateOptionsMenu();
    }

    @Override
    public void setAddOptionAvailable(final boolean addOptionAvailable) {
        this.mAddOptionAvailable = addOptionAvailable;
        if(getActivity() == null)
            return;
        super.runOnUiThread( new Runnable() {
            public void run() {
                rootContainer.findViewById(R.id.fragment_catalog_addbutton).setVisibility(
                        addOptionAvailable ? View.VISIBLE : View.GONE);
            }
        });
    }

    public class OPDSRecyclerAdapter extends RecyclerView.Adapter<OPDSRecyclerAdapter.ViewHolder> {

        private List<UstadJSOPDSEntry> entryList;

        public OPDSRecyclerAdapter(List<UstadJSOPDSEntry> entryList) {
            this.entryList = entryList;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public OPDSEntryCard mEntryCard;

            public ViewHolder(OPDSEntryCard entryCard) {
                super(entryCard);
                mEntryCard = entryCard;
            }
        }

        @Override
        public OPDSRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            OPDSEntryCard cardView  = (OPDSEntryCard) LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.fragment_opds_item, null);
            return new ViewHolder(cardView);
        }

        @Override
        public void onBindViewHolder(OPDSRecyclerAdapter.ViewHolder holder, int position) {
            holder.mEntryCard.setOPDSEntry(entryList.get(position));
            holder.mEntryCard.setOnClickListener(CatalogOPDSFragment.this);
            holder.mEntryCard.setOnLongClickListener(CatalogOPDSFragment.this);
            String entryId = entryList.get(position).id;
            if(idToStatusMap.containsKey(entryId)){
                holder.mEntryCard.setOPDSEntryOverlay(idToStatusMap.get(entryId));
            }else {
                holder.mEntryCard.setOPDSEntryOverlay(CatalogPresenter.STATUS_NOT_ACQUIRED);
            }

            if(idToProgressVisibleMap.containsKey(entryId) && idToProgressVisibleMap.get(entryId)) {
                holder.mEntryCard.setProgressBarVisible(true);
            }else {
                holder.mEntryCard.setProgressBarVisible(false);
            }

            //check the acquisition status
//            int entryStatus = controller.getEntryAcquisitionStatus(feed.entries[position].id);
//            holder.mEntryCard.setOPDSEntryOverlay(entryStatus);

            //Make sure if this entry is being recycled the idToCardMap won't get confused
            if(idToCardMap.containsValue(holder.mEntryCard)) {
                Iterator<Map.Entry<String, OPDSEntryCard>> iterator = idToCardMap.entrySet().iterator();
                Map.Entry<String, OPDSEntryCard> entry;
                while(iterator.hasNext()) {
                    entry = iterator.next();
                    if(entry.getValue().equals(holder.mEntryCard)) {
                        iterator.remove();
                    }
                }
            }

            if(idToThumbnailUrlMap.containsKey(entryId)) {
                holder.mEntryCard.setThumbnailUrl(idToThumbnailUrlMap.get(entryId),
                        mCatalogPresenter, CatalogOPDSFragment.this);
            }

            idToCardMap.put(entryList.get(position).id, holder.mEntryCard);


        }

        public int getItemCount() {
            return entryList.size();
        }
    }

}

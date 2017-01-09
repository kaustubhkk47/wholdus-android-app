package com.wholdus.www.wholdusbuyerapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.adapters.AddressDisplayListViewAdapter;
import com.wholdus.www.wholdusbuyerapp.helperClasses.HelperFunctions;
import com.wholdus.www.wholdusbuyerapp.helperClasses.TODO;
import com.wholdus.www.wholdusbuyerapp.interfaces.ItemClickListener;
import com.wholdus.www.wholdusbuyerapp.interfaces.ProfileListenerInterface;
import com.wholdus.www.wholdusbuyerapp.interfaces.UserAddressInterface;
import com.wholdus.www.wholdusbuyerapp.loaders.BuyerAddressLoader;
import com.wholdus.www.wholdusbuyerapp.models.BuyerAddress;
import com.wholdus.www.wholdusbuyerapp.services.UserService;

import java.util.ArrayList;

/**
 * Created by kaustubh on 26/12/16.
 */

public class BuyerAddressFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<BuyerAddress>>, ItemClickListener {

    private TextView mNoAddressTextView;
    private ListView mAddressListView;
    private TextView mAddAddressTextView;
    private UserAddressInterface mListener;
    private BroadcastReceiver mUserServiceResponseReceiver;
    private final int USER_ADDRESS_DB_LOADER = 50;
    private ArrayList<BuyerAddress> mBuyerAddresses;
    AddressDisplayListViewAdapter mAddressDisplayListViewAdapter;
    //TODO Define all loaders at one place


    public BuyerAddressFragment(){

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (UserAddressInterface) context;
        } catch (ClassCastException cee) {
            cee.printStackTrace();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserServiceResponseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleAPIResponse();
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_buyer_address, container, false);

        initReferences(rootView);
        getActivity().getSupportLoaderManager().restartLoader(USER_ADDRESS_DB_LOADER, null, this);
        fetchDataFromServer();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(getString(R.string.user_data_updated));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mUserServiceResponseReceiver, intentFilter);
        Bundle args = getArguments();
        mListener.fragmentCreated(args.getString("fragment_title", "My Addresses"), false);
    }

    private void initReferences(ViewGroup rootView) {
        mAddressListView = (ListView) rootView.findViewById(R.id.address_list_view);
        mBuyerAddresses = new ArrayList<>();
        mAddressDisplayListViewAdapter = new AddressDisplayListViewAdapter(getActivity().getApplicationContext(), mBuyerAddresses, this);
        mAddressListView.setAdapter(mAddressDisplayListViewAdapter);

        mAddressListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BuyerAddress buyerAddress = mBuyerAddresses.get(i);
                mListener.addressClicked(buyerAddress.getAddressID(), buyerAddress.get_ID());
                }
            });

        mNoAddressTextView = (TextView) rootView.findViewById(R.id.no_address_text_view);
        mAddAddressTextView = (TextView) rootView.findViewById(R.id.add_address_text_view);
        mAddAddressTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.editAddress(-1, -1);
            }
        });

    }



    public void fetchDataFromServer(){
        Intent intent = new Intent(getContext(), UserService.class);
        intent.putExtra("TODO", TODO.FETCH_USER_PROFILE);
        getContext().startService(intent);
    }

    private void handleAPIResponse() {
        if (getActivity()!= null) {
            getActivity().getSupportLoaderManager().restartLoader(USER_ADDRESS_DB_LOADER, null, this);
        }
    }


    private void setViewForAddressListView(ArrayList<BuyerAddress> address) {
        if (address.isEmpty()) {
            mNoAddressTextView.setVisibility(View.VISIBLE);
        } else {
            mNoAddressTextView.setVisibility(View.GONE);
            mBuyerAddresses.clear();
            mBuyerAddresses.addAll(address);
            mAddressDisplayListViewAdapter.notifyDataSetChanged();
            HelperFunctions.setListViewHeightBasedOnChildren(mAddressListView);
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<BuyerAddress>> loader) {

    }

    @Override
    public void onLoadFinished(Loader<ArrayList<BuyerAddress>> loader, ArrayList<BuyerAddress> data) {
        setViewForAddressListView(data);
    }

    @Override
    public Loader<ArrayList<BuyerAddress>> onCreateLoader(int id, Bundle args) {
        return new BuyerAddressLoader(getContext(), -1, -1);
    }

    @Override
    public void itemClicked(View view, int position, int id) {
        BuyerAddress buyerAddress = mBuyerAddresses.get(position);
        mListener.editAddress(buyerAddress.getAddressID(), buyerAddress.get_ID());
    }
}
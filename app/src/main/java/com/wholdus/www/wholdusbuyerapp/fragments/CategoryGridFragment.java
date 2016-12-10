package com.wholdus.www.wholdusbuyerapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.adapters.CategoriesGridAdapter;
import com.wholdus.www.wholdusbuyerapp.interfaces.HomeListenerInterface;
import com.wholdus.www.wholdusbuyerapp.loaders.CategoriesGridLoader;
import com.wholdus.www.wholdusbuyerapp.models.Category;
import com.wholdus.www.wholdusbuyerapp.services.CatalogService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aditya on 10/12/16.
 * Fragment to Display Categories present on wholdus
 */

public class CategoryGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<Category>> {

    private static int CATEGORIES_LOADER = 0;
    private BroadcastReceiver mReceiver;
    private RecyclerView mRecyclerView;
    private CategoriesGridAdapter mCategoriesGridAdapter;
    private HomeListenerInterface mListener;

    public CategoryGridFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (HomeListenerInterface) context;
        } catch (ClassCastException cee) {
            Log.e(this.getClass().getSimpleName(), " must implement " + HomeListenerInterface.class.getSimpleName());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleBroadcastOnReceive();
            }
        };

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_categories_grid, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.categories_recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        getActivity().getSupportLoaderManager().initLoader(CATEGORIES_LOADER, null, this);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.fragmentCreated("All Categories", false);
        IntentFilter intentFilter = new IntentFilter(getString(R.string.categories_data_updated));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, intentFilter);
        fetchDataFromServer();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public Loader<ArrayList<Category>> onCreateLoader(int id, Bundle args) {
        return new CategoriesGridLoader(getContext());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Category>> loader, ArrayList<Category> data) {
        setDataToView(data);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Category>> loader) {

    }

    private void fetchDataFromServer() {
        Intent intent = new Intent(getContext(), CatalogService.class);
        intent.putExtra("TODO", R.integer.fetch_categories);
        getActivity().startService(intent);
    }

    private void handleBroadcastOnReceive() {
        // database has been updated
        // redraw the UI
        getActivity().getSupportLoaderManager().restartLoader(CATEGORIES_LOADER, null, this);
    }

    private void setDataToView(List<Category> categories) {
        mCategoriesGridAdapter = new CategoriesGridAdapter(getContext(), categories);
        mRecyclerView.setAdapter(mCategoriesGridAdapter);
    }
}
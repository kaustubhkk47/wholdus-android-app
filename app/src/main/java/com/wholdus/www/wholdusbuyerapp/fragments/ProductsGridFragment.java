package com.wholdus.www.wholdusbuyerapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.activities.CheckoutActivity;
import com.wholdus.www.wholdusbuyerapp.activities.StoreActivity;
import com.wholdus.www.wholdusbuyerapp.adapters.ProductsGridAdapter;
import com.wholdus.www.wholdusbuyerapp.helperClasses.FilterClass;
import com.wholdus.www.wholdusbuyerapp.interfaces.CategoryProductListenerInterface;
import com.wholdus.www.wholdusbuyerapp.interfaces.ItemClickListener;
import com.wholdus.www.wholdusbuyerapp.loaders.GridProductsLoader;
import com.wholdus.www.wholdusbuyerapp.models.GridProductModel;
import com.wholdus.www.wholdusbuyerapp.services.CatalogService;

import java.util.ArrayList;

/**
 * Created by aditya on 8/12/16.
 */

public class ProductsGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<GridProductModel>>,
        View.OnClickListener, ItemClickListener {

    private CategoryProductListenerInterface mListener;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String mFilters;
    private boolean mScrolling;
    private ArrayList<GridProductModel> mProducts;
    private ProductsGridAdapter mProductsGridAdapter;
    private RecyclerView mProductsRecyclerView;
    private int mPageNumber;
    private final int mLimit = 20;

    public static final int PRODUCTS_GRID_LOADER = 2;

    public ProductsGridFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (CategoryProductListenerInterface) context;
        } catch (ClassCastException cee) {
            Log.e(getClass().getSimpleName(), " must implement " + CategoryProductListenerInterface.class.getSimpleName());
        }
        Log.d(this.getClass().getSimpleName(), "onattach");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mProducts == null) {
            mProducts = new ArrayList<>();
        } else {
            Toast.makeText(getContext(), "products already when created - " + mProducts.size(), Toast.LENGTH_SHORT).show();
        }
        mPageNumber = 1;

        Log.d(this.getClass().getSimpleName(), "oncreate");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_products_grid, container, false);
        setHasOptionsMenu(true);

        Button filterButton = (Button) rootView.findViewById(R.id.filter_button);
        filterButton.setOnClickListener(this);

        Button sortButton = (Button) rootView.findViewById(R.id.sort_button);
        sortButton.setOnClickListener(this);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(false);
                updateData();
            }
        });

        mProductsRecyclerView = (RecyclerView) rootView.findViewById(R.id.products_recycler_view);
        mProductsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        Log.d(this.getClass().getSimpleName(), "onCreateview");
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mProductsGridAdapter == null) {
            mProductsGridAdapter = new ProductsGridAdapter(getContext(), mProducts, this);
            mProductsRecyclerView.setAdapter(mProductsGridAdapter);
        }

        updateData();
        Log.d(this.getClass().getSimpleName(), "onactivitycreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(this.getClass().getSimpleName(), "onstart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(this.getClass().getSimpleName(), "onresume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(this.getClass().getSimpleName(), "onpause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(this.getClass().getSimpleName(), "onstop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(this.getClass().getSimpleName(), "onDestryview");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        Log.d(this.getClass().getSimpleName(), "ondetach");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.default_action_buttons, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bar_checkout:
                startActivity(new Intent(getContext(), CheckoutActivity.class));
                break;
            case R.id.action_bar_store_home:
                startActivity(new Intent(getContext(), StoreActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<ArrayList<GridProductModel>> onCreateLoader(int id, Bundle args) {
        return new GridProductsLoader(getContext(), mPageNumber, mLimit);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<GridProductModel>> loader, ArrayList<GridProductModel> data) {
        int oldPosition = mProducts.size();
        mProducts.addAll(data);
        if (oldPosition == 0) {
            mProductsGridAdapter.notifyDataSetChanged();
        } else {
            mProductsGridAdapter.notifyItemRangeInserted(oldPosition, data.size());
        }


        Log.d("on load", mProducts.size() + "");
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<GridProductModel>> loader) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.filter_button:
                mListener.openFilter(true);
                break;
            case R.id.sort_button:
                SortBottomSheetFragment.newInstance().show(getChildFragmentManager(), "Sort");
                break;
        }
    }

    @Override
    public void itemClicked(int position, int id) {
        Toast.makeText(getContext(), position + "", Toast.LENGTH_SHORT).show();
    }

    public void updateData() {
        String filters = FilterClass.getFilterString();

        // if the filter is not same then set page number to 1 and clean current products
        // else increment pageNumber
        if (!filters.equals(mFilters)) {
            mFilters = filters;
            mPageNumber = 1;
            mProducts.clear();
        } else {
            mPageNumber++;
        }

        getActivity().getSupportLoaderManager().restartLoader(PRODUCTS_GRID_LOADER, null, this);
        fetchProductsFromServer();
    }

    private void fetchProductsFromServer() {
        Intent intent = new Intent(getContext(), CatalogService.class);
        intent.putExtra("TODO", R.integer.fetch_products);
        intent.putExtra("page_number", mPageNumber);
        intent.putExtra("items_per_page", mLimit);
        getActivity().startService(intent);
    }
}

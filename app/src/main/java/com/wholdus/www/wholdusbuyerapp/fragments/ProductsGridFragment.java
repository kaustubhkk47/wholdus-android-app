package com.wholdus.www.wholdusbuyerapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.activities.CartActivity;
import com.wholdus.www.wholdusbuyerapp.activities.ProductDetailActivity;
import com.wholdus.www.wholdusbuyerapp.activities.StoreActivity;
import com.wholdus.www.wholdusbuyerapp.adapters.ProductsGridAdapter;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.CatalogContract;
import com.wholdus.www.wholdusbuyerapp.decorators.GridItemDecorator;
import com.wholdus.www.wholdusbuyerapp.helperClasses.APIConstants;
import com.wholdus.www.wholdusbuyerapp.helperClasses.Constants;
import com.wholdus.www.wholdusbuyerapp.helperClasses.FilterClass;
import com.wholdus.www.wholdusbuyerapp.helperClasses.HelperFunctions;
import com.wholdus.www.wholdusbuyerapp.helperClasses.IntentFilters;
import com.wholdus.www.wholdusbuyerapp.helperClasses.ShareIntentClass;
import com.wholdus.www.wholdusbuyerapp.helperClasses.TODO;
import com.wholdus.www.wholdusbuyerapp.interfaces.CategoryProductListenerInterface;
import com.wholdus.www.wholdusbuyerapp.interfaces.ItemClickListener;
import com.wholdus.www.wholdusbuyerapp.loaders.GridProductsLoader;
import com.wholdus.www.wholdusbuyerapp.models.GridProductModel;
import com.wholdus.www.wholdusbuyerapp.services.BuyerProductService;
import com.wholdus.www.wholdusbuyerapp.services.CatalogService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by aditya on 8/12/16.
 */

public class ProductsGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<GridProductModel>>,
        View.OnClickListener, ItemClickListener, ProductsGridAdapter.OnLoadMoreListener, SwipeRefreshLayout.OnRefreshListener {

    private CategoryProductListenerInterface mListener;
    private ArrayList<GridProductModel> mProducts;
    private ProductsGridAdapter mAdapter;
    private BroadcastReceiver mReceiver;
    private ProgressBar mPageLoader;
    private LinearLayout mPageLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private GridLayoutManager mGridLayoutManager;
    private Snackbar mSnackbar;
    private TextView mNoProducts;

    private Queue<Integer> mRequestQueue;

    private String mFilters;
    private int mPageNumber, mTotalPages, mRecyclerViewPosition, mLoadMoreData, mActivePageCall;
    private boolean mLoaderLoading;
    private ArrayList<Integer> mResponseCodes;

    private final int PRODUCTS_GRID_LOADER = 2;
    private final int mLIMIT = 30;

    public ProductsGridFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (CategoryProductListenerInterface) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initResponseCodes(getArguments());
        resetVariables();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleOnBroadcastReceive(intent);
            }
        };
        IntentFilter intentFilter = new IntentFilter(IntentFilters.PRODUCT_DATA);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, intentFilter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_products_grid, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAdapter == null) {
            mAdapter = new ProductsGridAdapter(getContext(), mProducts, this, mRecyclerView);
            if (mProducts.size() != 0) {
                resetVariables();
            }
            loadData();
        } else if (mProducts.size() > 0) {
            mPageLoader.setVisibility(View.INVISIBLE);
        }
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnLoadMoreListener(this);
        Log.d(this.getClass().getSimpleName(), "onactivitycreated");
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPageLoader = (ProgressBar) view.findViewById(R.id.page_loader);
        mPageLayout = (LinearLayout) view.findViewById(R.id.page_layout);

        Button filterButton = (Button) view.findViewById(R.id.filter_button);
        filterButton.setOnClickListener(this);

        Button sortButton = (Button) view.findViewById(R.id.sort_button);
        sortButton.setOnClickListener(this);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.products_recycler_view);
        mRecyclerView.addItemDecoration(new GridItemDecorator(2,
                getResources().getDimensionPixelSize(R.dimen.card_margin_horizontal), true, 0));
        mGridLayoutManager = new GridLayoutManager(getContext(), 2);
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case 1:
                        return 2;
                    default:
                        return 1;
                }
            }
        });
        mRecyclerView.setLayoutManager(mGridLayoutManager);

        mNoProducts = (TextView) view.findViewById(R.id.no_products);
    }

    @Override
    public void onResume() {
        super.onResume();

        // restore recycler view position
        mGridLayoutManager.scrollToPosition(mRecyclerViewPosition);
    }

    @Override
    public void onPause() {
        super.onPause();
        // save recycler view position
        mRecyclerViewPosition = mGridLayoutManager.findFirstVisibleItemPosition();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
                startActivity(new Intent(getContext(), CartActivity.class));
                break;
            case R.id.action_bar_store_home:
                startActivity(new Intent(getContext(), StoreActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<ArrayList<GridProductModel>> onCreateLoader(int id, Bundle args) {
        mLoaderLoading = true;
        return new GridProductsLoader(getContext(), mPageNumber, mLIMIT, mResponseCodes);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<GridProductModel>> loader, ArrayList<GridProductModel> data) {
        mLoaderLoading = false;
        if (data.size() != 0) {
            if (mPageLoader.getVisibility() == View.VISIBLE) {
                mPageLoader.setVisibility(View.INVISIBLE);
                mPageLayout.setVisibility(View.VISIBLE);
            }

            final int oldPosition = mProducts.size();
            final int scrollerPosition = mGridLayoutManager.findFirstVisibleItemPosition();
            removeDummyObject();
            mProducts.addAll(data);
            //mAdapter.notifyItemRangeInserted(oldPosition, data.size());
            mAdapter.notifyDataSetChanged();
            if (scrollerPosition >= 0) {
                View firstVisible = mGridLayoutManager.findViewByPosition(scrollerPosition);
                mGridLayoutManager.scrollToPositionWithOffset(scrollerPosition, firstVisible.getTop());
            }
            mAdapter.setLoaded();
        } else if (!hasNextPage()) {
            removeDummyObject();
        }
        if (mLoadMoreData > 0) {
            mLoadMoreData--;
            loadData();
        }
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
    public void itemClicked(View view, int position, int id) {
        final int ID = id == -1 ? view.getId() : id;
        Intent intent;

        switch (ID) {
            case R.id.share_image_view:
                ShareIntentClass.shareImage(getContext(), (ImageView) view, mProducts.get(position).getName());
                break;
            case R.id.cart_image_view:
                CartDialogFragment cartDialog = new CartDialogFragment();
                Bundle args = new Bundle();
                args.putInt(CatalogContract.ProductsTable.COLUMN_PRODUCT_ID, mProducts.get(position).getProductID());
                cartDialog.setArguments(args);
                cartDialog.show(getFragmentManager(), cartDialog.getClass().getSimpleName());
                break;
            case R.id.fav_icon_image_view:
                GridProductModel product = mProducts.get(position);
                product.toggleLikeStatus();
                mAdapter.notifyItemChanged(position, product);

                intent = new Intent(getContext(), BuyerProductService.class);
                intent.putExtra("TODO", TODO.UPDATE_PRODUCT_RESPONSE);
                intent.putExtra(CatalogContract.ProductsTable.COLUMN_PRODUCT_ID, product.getProductID());
                intent.putExtra(CatalogContract.ProductsTable.COLUMN_RESPONDED_FROM, 1);
                intent.putExtra(CatalogContract.ProductsTable.COLUMN_HAS_SWIPED, false);
                intent.putExtra(CatalogContract.ProductsTable.COLUMN_RESPONSE_CODE, product.getLikeStatus() ? 1 : 2);

                getContext().startService(intent);
                break;
            default:
                intent = new Intent(getContext(), ProductDetailActivity.class);
                intent.putExtra(CatalogContract.ProductsTable.TABLE_NAME, mProducts.get(position).getProductID());
                startActivity(intent);
        }
    }

    @Override
    public void onLoadMore() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mLoadMoreData++;
                Log.d(ProductsGridFragment.class.getSimpleName(), "Total products count: " + mProducts.size());
                if (!mLoaderLoading) {
                    mLoadMoreData--;
                    loadData();
                }
            }
        });
    }

    @Override
    public void onRefresh() {
        final int oldTotalPages = mTotalPages;
        resetVariables();
        mPageNumber = 0;
        mTotalPages = oldTotalPages;
        loadData();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void loadData() {
        String filters = FilterClass.getFilterString();

        // if the filter is not same then set page number to 1 and clean current products
        // else increment pageNumber
        if (!filters.equals(mFilters)) {
            mFilters = filters;
            resetVariables();
        } else {
            mPageNumber++;
        }

        if (hasNextPage()) {
            if (mProducts.size() > 0 && mProducts.get(mProducts.size() - 1) != null) {
                mProducts.add(null);
                mAdapter.notifyItemInserted(mProducts.size() - 1);
            }
            mRequestQueue.add(mPageNumber);
            fetchProductsFromServer();
            getActivity().getSupportLoaderManager().restartLoader(PRODUCTS_GRID_LOADER, null, this);
        }
    }

    private void initResponseCodes(Bundle args) {
        final int type = args.getInt(Constants.TYPE);
        mResponseCodes = new ArrayList<>();
        switch (type) {
            case Constants.FAV_PRODUCTS:
                mResponseCodes.add(Constants.FAV_PRODUCTS);
                break;
            case Constants.REJECTED_PRODUCTS:
                mResponseCodes.add(Constants.REJECTED_PRODUCTS);
                break;
            default:
                mResponseCodes.add(Constants.FAV_PRODUCTS);
                mResponseCodes.add(Constants.REJECTED_PRODUCTS);
                mResponseCodes.add(Constants.ALL_PRODUCTS);
        }
    }

    public void resetVariables() {
        mPageNumber = 1;
        mTotalPages = -1;
        mRecyclerViewPosition = 0;
        mLoadMoreData = 0;
        mActivePageCall = 0;

        if (mRequestQueue == null) {
            mRequestQueue = new LinkedList<>();
        } else {
            mRequestQueue.clear();
        }

        if (mProducts == null) {
            mProducts = new ArrayList<>();
        } else {
            resetAdapterState();
        }
    }

    private boolean hasNextPage() {
        return (mTotalPages == -1 || mPageNumber <= mTotalPages);
    }

    private void fetchProductsFromServer() {
        if (mRequestQueue.size() > 0) {
            int pageNumber = mRequestQueue.element();
            if (pageNumber == mActivePageCall) {
                return;
            }
            mActivePageCall = mPageNumber;

            if (mResponseCodes.size() == 1) {
                Intent bpResponse = new Intent(getContext(), BuyerProductService.class);
                bpResponse.putExtra("TODO", TODO.FETCH_BUYER_PRODUCTS_RESPONSE);
                bpResponse.putExtra(APIConstants.API_ITEM_PER_PAGE_KEY, mLIMIT);
                bpResponse.putExtra(APIConstants.API_PAGE_NUMBER_KEY, pageNumber);
                bpResponse.putExtra(APIConstants.API_RESPONSE_CODE_KEY, TextUtils.join(",", mResponseCodes));
                getActivity().startService(bpResponse);
            } else {
                Intent intent = new Intent(getContext(), CatalogService.class);
                intent.putExtra("TODO", R.integer.fetch_products);
                intent.putExtra(APIConstants.API_PAGE_NUMBER_KEY, pageNumber);
                intent.putExtra(APIConstants.API_ITEM_PER_PAGE_KEY, mLIMIT);
                getActivity().startService(intent);
            }
        }
    }

    private void handleOnBroadcastReceive(final Intent intent) {
        if (mRequestQueue.size() > 0) {
            mRequestQueue.remove();
        }

        Bundle extras = intent.getExtras();
        if (extras.getString(Constants.ERROR_RESPONSE) != null) {
            if (mPageLoader.getVisibility() == View.VISIBLE) {
                mPageLoader.setVisibility(View.INVISIBLE);
                showErrorMessage();
            }
            return;
        }
        final int pageNumber = intent.getIntExtra(APIConstants.API_PAGE_NUMBER_KEY, -1);
        final int totalPages = intent.getIntExtra(APIConstants.API_TOTAL_PAGES_KEY, 1);
        final int updatedInserted = intent.getIntExtra(Constants.INSERTED_UPDATED, 0);
        Log.d(this.getClass().getSimpleName(), "page number from api: " + pageNumber);
        Log.d(this.getClass().getSimpleName(), "total pages: " + totalPages);
        if (updatedInserted > 0) {
            // if loader is active means no product in local, simply show
            if (mPageLoader.getVisibility() == View.VISIBLE) {
                resetVariables();
                getActivity().getSupportLoaderManager().restartLoader(PRODUCTS_GRID_LOADER, null, this);
            }
            // if page number is 1 then flicker and reload data
            else if (mPageNumber == 1 && pageNumber == 1) {
                resetVariables();
                final int oldPosition = mProducts.size();
                if (mPageNumber == 1 && oldPosition > 0) {
                    mProducts.clear();
                    mAdapter.notifyItemRangeRemoved(0, oldPosition);
                }
                getActivity().getSupportLoaderManager().restartLoader(PRODUCTS_GRID_LOADER, null, this);
                Toast.makeText(getContext(), getString(R.string.products_updated), Toast.LENGTH_LONG).show();
            } else {
                final int scrollPosition = mGridLayoutManager.findLastVisibleItemPosition();
                if (scrollPosition >= mProducts.size() - 2 || pageNumber == mPageNumber) {
                    getActivity().getSupportLoaderManager().restartLoader(PRODUCTS_GRID_LOADER, null, this);
                } else {
                    showSnackbarToUpdateUI(totalPages);
                }
            }
        } else if (updatedInserted == -1) {
            if (mNoProducts.getVisibility() == View.GONE) {
                mNoProducts.setVisibility(View.VISIBLE);
                mSwipeRefreshLayout.setVisibility(View.GONE);
            }
        }

        mTotalPages = totalPages;
        if (mTotalPages == 1) {
            removeDummyObject();
        } else if (mRequestQueue.size() > 0) {
            fetchProductsFromServer();
        }
    }

    private void resetAdapterState() {
        int totalItems = mProducts.size();
        if (totalItems > 0) {
            mProducts.clear();
            if (mAdapter != null) {
                mAdapter.notifyItemRangeRemoved(0, totalItems);
            }
        }
    }

    private void removeDummyObject() {
        if (mProducts.size() > 0 && mProducts.get(mProducts.size()-1) == null) {
            mProducts.remove(mProducts.size() - 1);
            mAdapter.notifyItemRemoved(mProducts.size());
        }
    }

    private void showErrorMessage() {
        if (mSnackbar != null && mSnackbar.isShownOrQueued()) {
            mSnackbar.dismiss();
        }

        final View view = getView();
        if (view == null) {
            Log.d(ProductsGridFragment.class.getSimpleName(), "get view was null");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                if (HelperFunctions.isNetworkAvailable(getContext())) {
                    message = getString(R.string.api_error_message);
                } else {
                    message = getString(R.string.no_internet_access);
                }
                mSnackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.retry_text, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (mPageNumber == 1) {
                                    mPageLoader.setVisibility(View.VISIBLE);
                                }
                                fetchProductsFromServer();
                            }
                        });

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) mSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
                        mSnackbar.show();
                    }
                });
            }
        }).start();
    }

    private void showSnackbarToUpdateUI(final int totalPages) {
        if (mSnackbar != null && mSnackbar.isShownOrQueued()) {
            mSnackbar.dismiss();
        }

        final View view = getView();
        if (view == null) {
            Log.d(ProductsGridFragment.class.getSimpleName(), "get view was null");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                mSnackbar = Snackbar.make(view, getString(R.string.new_products_message), Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.show_text, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                resetVariables();
                                mTotalPages = totalPages;
                                getActivity().getSupportLoaderManager().restartLoader(PRODUCTS_GRID_LOADER, null, ProductsGridFragment.this);
                            }
                        });

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) mSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
                        mSnackbar.show();
                    }
                });
            }
        }).start();
    }
}

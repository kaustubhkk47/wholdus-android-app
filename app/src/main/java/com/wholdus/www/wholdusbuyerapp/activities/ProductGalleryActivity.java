package com.wholdus.www.wholdusbuyerapp.activities;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.adapters.ProductGalleryViewPagerAdapter;
import com.wholdus.www.wholdusbuyerapp.adapters.ThumbImageAdapter;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.CatalogContract;
import com.wholdus.www.wholdusbuyerapp.helperClasses.Constants;
import com.wholdus.www.wholdusbuyerapp.interfaces.ItemClickListener;
import com.wholdus.www.wholdusbuyerapp.loaders.ProductLoader;
import com.wholdus.www.wholdusbuyerapp.models.Product;

public class ProductGalleryActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Product>, View.OnClickListener, ItemClickListener {

    private Toolbar mToolbar;
    private RecyclerView mThumbImagesView;
    private ViewPager mViewPager;

    private int mProductID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_gallery);

        initToolbar();

        setProductID();

        mViewPager = (ViewPager) findViewById(R.id.gallery_view_pager);

        mThumbImagesView = (RecyclerView) findViewById(R.id.thumb_images_recycler_view);
        mThumbImagesView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        getSupportLoaderManager().initLoader(100, null, this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onClick(View view) {
        final int ID = view.getId();
        switch (ID) {
            case R.id.gallery_view_pager:
                if (mThumbImagesView.getVisibility() == View.VISIBLE) {
                    mThumbImagesView.setVisibility(View.GONE);
                } else {
                    mThumbImagesView.setVisibility(View.VISIBLE);
                }
        }
    }

    @Override
    public void itemClicked(int position, int id) {
        mViewPager.setCurrentItem(position);
    }

    @Override
    public Loader<Product> onCreateLoader(int id, Bundle args) {
        return new ProductLoader(this, mProductID);
    }

    @Override
    public void onLoadFinished(Loader<Product> loader, Product data) {
        if (data != null) {
            ProductGalleryViewPagerAdapter viewPagerAdapter = new ProductGalleryViewPagerAdapter(getSupportFragmentManager(), this, data.getAllImageUrls(Constants.LARGE_IMAGE));
            mViewPager.setAdapter(viewPagerAdapter);
            mThumbImagesView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    outRect.left = 15;
                }
            });
            mThumbImagesView.setAdapter(new ThumbImageAdapter(this, data.getAllImageUrls(Constants.THUMB_IMAGE), this));
        } else {
            onBackPressed();
        }
    }

    @Override
    public void onLoaderReset(Loader<Product> loader) {

    }

    private void setProductID() {
        mProductID = getIntent().getIntExtra(CatalogContract.ProductsTable.TABLE_NAME, 0);
        if (mProductID == 0) {
            Log.d(this.getClass().getSimpleName(), mProductID + " - this is not a valid product ID");
            onBackPressed();
        }
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.default_toolbar);
        setSupportActionBar(mToolbar);

        mToolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_left_white_32dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }
}

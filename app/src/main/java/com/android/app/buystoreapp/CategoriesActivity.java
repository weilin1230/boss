package com.android.app.buystoreapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.app.buystore.utils.SharedPreferenceUtils;
import com.android.app.buystoreapp.adapter.GoodsLeftAdapter;
import com.android.app.buystoreapp.adapter.GoodsRightAdapter;
import com.android.app.buystoreapp.base.BaseAct;
import com.android.app.buystoreapp.bean.CommodityCategory;
import com.android.app.buystoreapp.bean.CommoditySubCategory;
import com.android.app.buystoreapp.bean.GsonCommodityBack;
import com.android.app.utils.HttpUtils;
import com.android.app.utils.LogUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lidroid.xutils.util.LogUtils;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类
 * Created by likaihang on 16/08/15.
 */
public class CategoriesActivity extends BaseAct implements GoodsRightAdapter.GoodsRightItemListener {
    private String cityID;
    private String url;
    private ListView mGoodsLeftListView;
    private ListView mGoodsRightListView;
    private GoodsLeftAdapter mGoodsLeftAdapter;
    private GoodsRightAdapter mGoodsRightAdapter;
    private List<CommodityCategory> mCommodityCategoriesList = new ArrayList<CommodityCategory>();
    private List<CommoditySubCategory> mCommoditySubCategoriesList = new ArrayList<CommoditySubCategory>();
    private AdapterView.OnItemClickListener mGoodsLeftListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("ResourceAsColor")
        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int position,
                                long arg3) {
            updateDataForRight(position);
        }
    };
    private AdapterView.OnItemSelectedListener mGoodsLeftSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> arg0, View view, int position,
                                   long arg3) {
            mGoodsLeftAdapter.setSelectItem(position);
            mGoodsLeftAdapter.notifyDataSetInvalidated();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.categories_layout);
        ((TextView)findViewById(R.id.tv_title)).setText(getResources().getString(R.string.zy_fw));
        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CategoriesActivity.this.finish();
            }
        });
        mGoodsLeftListView = (ListView) findViewById(R.id.id_goods_left_list);
        mGoodsLeftAdapter = new GoodsLeftAdapter(this, mCommodityCategoriesList);
        mGoodsLeftListView.setAdapter(mGoodsLeftAdapter);
        mGoodsLeftListView.setOnItemClickListener(mGoodsLeftListener);
        mGoodsLeftListView.setOnItemSelectedListener(mGoodsLeftSelectedListener);

        mGoodsRightListView = (ListView) findViewById(R.id.id_goods_right_list);
        mGoodsRightAdapter = new GoodsRightAdapter(this, mCommoditySubCategoriesList, this);
        mGoodsRightListView.setAdapter(mGoodsRightAdapter);

        cityID = SharedPreferenceUtils.getCurrentCityInfo(this).getId();
        if ("".equals(cityID)) {
            /*Intent intent = new Intent(getActivity(), CityActivity.class);
            startActivityForResult(intent, HANDLE_REQUEST_CITY);*/
            cityID = "110100";
        }
        url = getString(R.string.web_url);
        initErrorPage();
        addIncludeLoading(true);
        startWhiteLoadingAnim();
        load();
    }

    @Override
    protected void load() {
        requestCommodity();
    }


    private void initDataForLeft() {
        mGoodsLeftAdapter.notifyDataSetChanged();
        updateDataForRight(0);
    }

    private void updateDataForRight(int i) {
        if (mCommodityCategoriesList.get(i).getSubCategoryList().size() == 0) {
            mCommoditySubCategoriesList.clear();
            mGoodsRightAdapter.notifyDataSetChanged();
        } else {
            mCommoditySubCategoriesList.clear();
            mCommoditySubCategoriesList.addAll(mCommodityCategoriesList.get(i).getSubCategoryList());
            mGoodsRightAdapter.notifyDataSetChanged();
        }
    }

    private void requestCommodity() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("cmd", "getCommodityCategoty");
            obj.put("cityID", cityID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LogUtils.d("requestCommodity param=" + obj.toString());
        HttpUtils.post(mContext, obj, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] arg2) {
                LogUtil.LogAll("三级分类返回数据--",new String(arg2));
                stopLoadingAnim();
                hideErrorPageState();
                try {
                    Gson gson = new Gson();
                    GsonCommodityBack gsonCommodityBack = gson.fromJson(new String(
                            arg2), new TypeToken<GsonCommodityBack>() {
                    }.getType());

                    String result = gsonCommodityBack.getResult();
                    if ("0".equals(result)) {
                        if (gsonCommodityBack
                                .getCommodityCategoryList().size() == 0) {
                        } else {
                            mCommodityCategoriesList.addAll(gsonCommodityBack
                                    .getCommodityCategoryList());
                            initDataForLeft();
                        }
                    } else {
                        showErrorPageState(SERVEICE_ERR_FLAG);
                    }
                } catch (Exception e) {
                    LogUtils.e("requestCommodity error:", e);
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                showErrorPageState(SERVEICE_ERR_FLAG);
            }
        }, new HttpUtils.RequestNetworkError() {
            @Override
            public void networkError() {
                showErrorPageState(SERVEICE_ERR_FLAG);
            }
        });
    }

    @Override
    public void onItemClick(String categoryID, String categoryName) {
        Intent intent = new Intent(this,
                ProductListActivity.class);
        intent.putExtra("categoryID", categoryID);
        intent.putExtra("cityID", cityID);
        intent.putExtra("threeCategoryName",categoryName);
        intent.putExtra("where", 1);
        this.startActivity(intent);
    }
}

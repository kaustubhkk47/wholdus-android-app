package com.wholdus.www.wholdusbuyerapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.UserProfileContract;
import com.wholdus.www.wholdusbuyerapp.helperClasses.APIConstants;
import com.wholdus.www.wholdusbuyerapp.helperClasses.HelperFunctions;
import com.wholdus.www.wholdusbuyerapp.helperClasses.InputValidationHelper;
import com.wholdus.www.wholdusbuyerapp.helperClasses.IntentFilters;
import com.wholdus.www.wholdusbuyerapp.helperClasses.TODO;
import com.wholdus.www.wholdusbuyerapp.interfaces.LoginSignupListenerInterface;
import com.wholdus.www.wholdusbuyerapp.services.FirebaseNotificationService;
import com.wholdus.www.wholdusbuyerapp.services.LoginAPIService;

/**
 * Created by aditya on 16/11/16.
 */

public class LoginFragment extends Fragment implements View.OnClickListener, View.OnFocusChangeListener {

    private LoginSignupListenerInterface mListener;
    private TextInputLayout mMobileNumberWrapper, mPasswordWrapper;
    private TextInputEditText mMobileNumberEditText, mPasswordEditText;
    private ProgressBar mProgressBar;

    private BroadcastReceiver mReceiver;

    public LoginFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (LoginSignupListenerInterface) context;
        } catch (ClassCastException cce) {
            Log.d(this.getClass().getSimpleName(), " must implement mListener interface");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleAPIResponse(intent);
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_login, container, false);
        initFragment(rootView);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(IntentFilters.LOGIN_SIGNUP_DATA);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        final int ID = view.getId();
        mListener.hideSoftKeyboard(view);

        switch (ID) {
            case R.id.login_submit_button:
                handleLoginSubmitButtonClick();
                break;
            case R.id.signup_text:
                mListener.singupClicked(getValueFromEditText(mMobileNumberEditText));
                break;
            case R.id.forgot_password_textView:
                mListener.forgotPasswordClicked(getValueFromEditText(mMobileNumberEditText));
                break;
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        final String emptyText = getValueFromEditText((TextInputEditText) view);

        if (emptyText != null && ((!hasFocus && emptyText.isEmpty()) || (hasFocus && emptyText.isEmpty()))) {
            ((TextInputEditText) view).setError(null);
        }
    }

    private void initFragment(ViewGroup rootView) {

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.INVISIBLE);

        Button loginSubmitButton = (Button) rootView.findViewById(R.id.login_submit_button);
        loginSubmitButton.setOnClickListener(this);

        TextView signupText = (TextView) rootView.findViewById(R.id.signup_text);
        signupText.setOnClickListener(this);

        TextView forgotPasswordTextView = (TextView) rootView.findViewById(R.id.forgot_password_textView);
        forgotPasswordTextView.setOnClickListener(this);

        mMobileNumberWrapper = (TextInputLayout) rootView.findViewById(R.id.mobile_number_wrapper);
        mPasswordWrapper = (TextInputLayout) rootView.findViewById(R.id.password_wrapper);

        mMobileNumberEditText = (TextInputEditText) rootView.findViewById(R.id.mobile_number_edit_text);
        mMobileNumberEditText.setOnFocusChangeListener(this);

        mPasswordEditText = (TextInputEditText) rootView.findViewById(R.id.password_edit_text);
        mPasswordEditText.setOnFocusChangeListener(this);
    }

    private String getValueFromEditText(TextInputEditText editText) {
        try {
            return editText.getText().toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void handleLoginSubmitButtonClick() {
        String password = getValueFromEditText(mPasswordEditText);
        String mobileNumber = getValueFromEditText(mMobileNumberEditText);

        if (InputValidationHelper.isValidMobileNumber(mMobileNumberWrapper, mobileNumber)
                && InputValidationHelper.isValidPassword(mPasswordWrapper, password)) {

            if (mProgressBar.getVisibility() == View.VISIBLE) {
                return;
            }
            mProgressBar.setVisibility(View.VISIBLE);

            Intent intent = new Intent(getContext(), LoginAPIService.class);
            intent.putExtra("TODO", TODO.LOGIN);
            intent.putExtra(UserProfileContract.UserTable.COLUMN_MOBILE_NUMBER, mobileNumber);
            intent.putExtra(UserProfileContract.UserTable.COLUMN_PASSWORD, password);

            getActivity().startService(intent);
        }
    }

    private void handleAPIResponse(Intent intent) {
        mProgressBar.setVisibility(View.INVISIBLE);
        Bundle extras = intent.getExtras();
        int todo = extras.getInt("TODO", -1);
        String data = extras.getString(APIConstants.LOGIN_API_DATA);

        if (todo != TODO.LOGIN) {
            return;
        }

        int responseCode = extras.getInt(APIConstants.RESPONSE_CODE);
        switch (responseCode) {
            case 200:
                getActivity().startService(new Intent(getActivity().getApplicationContext(), FirebaseNotificationService.class));
                mListener.loginSuccess();
                break;
            case 401:
                mMobileNumberWrapper.setError(data);
                break;
            case 403:
                mMobileNumberWrapper.setError(data);
                break;
            default:
                if (HelperFunctions.isNetworkAvailable(getActivity().getApplicationContext())) {
                    if (data != null && data.contains("timeout")) {
                        Toast.makeText(getActivity().getApplicationContext(), getString(R.string.timeout_error), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(), getString(R.string.api_error_message), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.no_internet_access), Toast.LENGTH_SHORT).show();
                }
        }
    }
}

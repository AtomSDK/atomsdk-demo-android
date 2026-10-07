/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.atom.vpn.demo.AtomDemoApplicationController;
import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.CoachMarks;
import com.atom.vpn.demo.common.Constants;
import com.atom.vpn.demo.common.UIHelper;
import com.atom.vpn.demo.common.logger.Log;

import de.blinkt.openvpn.BuildConfig;


public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";

    private static final String STATE_PASSWORD_VISIBLE = "password_visible";

    private EditText etUsername;
    private EditText etPassword;
    private ImageView ivTogglePassword;

    private boolean passwordVisible;

    /** The system VPN permission dialog, shown by Initialize SDK until the user has granted it. */
    private final ActivityResultLauncher<Intent> vpnPermission = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.i(TAG, "VPN API Permission provided");
                    if (etUsername != null) {
                        navigateToHome();
                    }
                } else {
                    Log.e(TAG, "VPN API Permission cancelled");
                    UIHelper.showAlertDialog(requireContext(), getString(R.string.vpn_permission_required));
                }
            });

    /** Redraws the status chip once the asynchronous AtomManager.initialize() finishes. */
    private final AtomDemoApplicationController.OnInitializedListener onSdkInitialized =
            atomManager -> {
                if (getView() != null) {
                    updateSetupStatus(getView());
                }
            };

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            passwordVisible = savedInstanceState.getBoolean(STATE_PASSWORD_VISIBLE, false);
        }

        ((TextView) view.findViewById(R.id.tvSecondary)).setText(
                getString(R.string.sdk_version_format, getString(R.string.app_name), BuildConfig.SDKVersion));

        // Read-only: the key is baked into the build, the user cannot edit it here.
        TextView tvSecretKey = view.findViewById(R.id.tvSecretKey);
        String secretKey = getString(R.string.atom_secret_key);
        tvSecretKey.setText(TextUtils.isEmpty(secretKey)
                ? getString(R.string.empty_secret_key)
                : secretKey);

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);

        ivTogglePassword = view.findViewById(R.id.ivTogglePassword);
        ivTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            applyPasswordVisibility();
        });
        applyPasswordVisibility();

        view.findViewById(R.id.btnClear).setOnClickListener(v -> clearCredentials());
        view.findViewById(R.id.btnInitialize).setOnClickListener(v -> openHomeScreen());

        CoachMarks.set(ivTogglePassword, R.string.coach_toggle_password);
        CoachMarks.set(view.findViewById(R.id.btnClear), R.string.coach_clear_credentials);
        CoachMarks.set(view.findViewById(R.id.btnInitialize), R.string.coach_initialize_sdk);

        updateSetupStatus(view);
        AtomDemoApplicationController.getInstance().addOnInitializedListener(onSdkInitialized);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Also re-read on return, in case the state changed while another screen was on top.
        if (getView() != null) {
            updateSetupStatus(getView());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PASSWORD_VISIBLE, passwordVisible);
    }

    @Override
    public void onDestroyView() {
        AtomDemoApplicationController.getInstance().removeOnInitializedListener(onSdkInitialized);
        super.onDestroyView();
        etUsername = null;
        etPassword = null;
        ivTogglePassword = null;
    }

    private void updateSetupStatus(@NonNull View view) {
        view.findViewById(R.id.initialSetupHeaderStatus).setBackgroundResource(R.drawable.bg_not_setup_step);

        view.findViewById(R.id.vStatusDot).setBackgroundResource(R.drawable.bg_red_circle);

        TextView status = view.findViewById(R.id.tvSetupStatus);
        status.setText(R.string.not_initialized);
        status.setTextColor(ContextCompat.getColor(requireContext(), R.color.not_font_color));
    }

    private void clearCredentials() {
        etUsername.setText("");
        etPassword.setText("");
        etUsername.setError(null);
        etPassword.setError(null);
        etUsername.requestFocus();
    }

    /**
     * Swaps the transformation method rather than the input type: changing inputType resets the
     * typeface, which would make the field jump to monospace mid-edit.
     */
    private void applyPasswordVisibility() {
        int selection = etPassword.getSelectionEnd();
        etPassword.setTransformationMethod(
                passwordVisible ? null : PasswordTransformationMethod.getInstance());
        if (selection >= 0) {
            etPassword.setSelection(selection);
        }
        ivTogglePassword.setContentDescription(
                getString(passwordVisible ? R.string.cd_hide_password : R.string.cd_show_password));
        ivTogglePassword.setSelected(passwordVisible);
    }

    /**
     * Step 1 validates the credentials and hands them to {@link HomeFragment}, which owns the
     * "connect with ..." picker and launches ConnectActivity with the chosen method.
     */
    private void openHomeScreen() {
        if (TextUtils.isEmpty(etUsername.getText().toString().trim())) {
            etUsername.setError(Constants.UsernameRequired);
        } else if (TextUtils.isEmpty(etPassword.getText().toString().trim())) {
            etUsername.setError(null);
            etPassword.setError(Constants.PasswordRequired);
        } else {

            etUsername.setError(null);
            etPassword.setError(null);

            if (AtomDemoApplicationController.getInstance().getAtomManager() == null) {
                Toast.makeText(getActivity(), R.string.atom_manager_not_initialized, Toast.LENGTH_LONG).show();
                return;
            }

            // VpnService.prepare returns null once the user has granted the permission.
            Intent prepare = VpnService.prepare(requireContext());
            if (prepare == null) {
                navigateToHome();
            } else {
                vpnPermission.launch(prepare);
            }
        }
    }

    private void navigateToHome() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.sample_content_fragment, HomeFragment.newInstance(
                        etUsername.getText().toString(), etPassword.getText().toString()))
                .addToBackStack(null)
                .commit();
    }

}

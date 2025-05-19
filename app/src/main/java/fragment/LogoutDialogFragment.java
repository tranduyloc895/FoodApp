package fragment;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.appfood.R;
import com.example.appfood.SignInActivity;

import api.ApiService;
import api.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DialogFragment for confirming and handling user logout.
 * Handles API logout, navigation, and error reporting.
 */
public class LogoutDialogFragment extends DialogFragment {

    private String token;

    /**
     * Factory method to create a new instance of LogoutDialogFragment with a token.
     * @param token The authentication token.
     * @return A new instance of LogoutDialogFragment.
     */
    public static LogoutDialogFragment newInstance(String token) {
        LogoutDialogFragment fragment = new LogoutDialogFragment();
        Bundle args = new Bundle();
        args.putString("token", token);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates the dialog, sets up UI and button listeners.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        token = getArguments() != null ? getArguments().getString("token") : null;
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.activity_logout_dialog, null);

        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnLogout = view.findViewById(R.id.btn_confirm_logout);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnLogout.setOnClickListener(v -> handleLogout(dialog));

        return dialog;
    }

    /**
     * Handles the logout process: calls API, navigates to sign-in, and handles errors.
     * @param dialog The dialog to dismiss on completion.
     */
    private void handleLogout(AlertDialog dialog) {
        if (token == null) {
            Toast.makeText(getActivity(), "Token không tồn tại.", Toast.LENGTH_SHORT).show();
            return;
        }
        ApiService apiService = RetrofitClient.getApiService();
        Call<Void> call = apiService.logout("Bearer " + token);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    navigateToSignIn();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getActivity(), "Đăng xuất thất bại.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getActivity(), "Lỗi kết nối.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Navigates to the SignInActivity and clears the activity stack.
     */
    private void navigateToSignIn() {
        Intent intent = new Intent(getActivity(), SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Sets dialog window background and layout for full-screen effect.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    /**
     * Applies a custom dialog theme.
     */
    @Override
    public int getTheme() {
        return R.style.CustomDialogTheme;
    }
}

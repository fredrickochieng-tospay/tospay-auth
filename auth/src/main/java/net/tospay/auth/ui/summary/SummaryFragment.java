package net.tospay.auth.ui.summary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;

import net.tospay.auth.BR;
import net.tospay.auth.R;
import net.tospay.auth.Tospay;
import net.tospay.auth.databinding.FragmentSummaryBinding;
import net.tospay.auth.model.TospayUser;
import net.tospay.auth.model.transfer.Transfer;
import net.tospay.auth.remote.Resource;
import net.tospay.auth.remote.ServiceGenerator;
import net.tospay.auth.remote.repository.PaymentRepository;
import net.tospay.auth.remote.exception.TospayException;
import net.tospay.auth.remote.service.PaymentService;
import net.tospay.auth.ui.auth.AuthActivity;
import net.tospay.auth.ui.base.BaseFragment;
import net.tospay.auth.utils.NetworkUtils;
import net.tospay.auth.viewmodelfactory.PaymentViewModelFactory;

import static net.tospay.auth.utils.Constants.KEY_TOKEN;

public class SummaryFragment extends BaseFragment<FragmentSummaryBinding, SummaryViewModel>
        implements SummaryNavigator, LoginTypeDialog.OnLoginTypeListener {

    private FragmentSummaryBinding mBinding;
    private String paymentId;
    private SummaryViewModel mViewModel;
    private Tospay tospay;
    private TospayUser tospayUser;
    private Transfer transfer;

    public SummaryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.tospay = Tospay.getInstance(getContext());
        this.tospayUser = tospay.getCurrentUser();
        if (getArguments() != null) {
            this.paymentId = getArguments().getString(KEY_TOKEN);
        }
    }

    @Override
    public void onDestroy() {
        mBinding.unbind();
        super.onDestroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding = getViewDataBinding();
        mViewModel = getViewModel();
        mViewModel.setNavigator(this);
        mBinding.setSummaryViewModel(mViewModel);

        mViewModel.getUser().setValue(tospayUser);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mListener.onPaymentFailed(new TospayException("Transaction canceled"));
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(callback);

        fetchPaymentDetails();

        mBinding.btnSendPayment.setOnClickListener(view1 -> {
            if (tospayUser == null) {
                LoginTypeDialog.newInstance().show(getChildFragmentManager(), LoginTypeDialog.TAG);
                return;
            }

            if (tospay.getSharedPrefManager(getContext()).isTokenExpiredOrAlmost()) {
                LoginTypeDialog.newInstance().show(getChildFragmentManager(), LoginTypeDialog.TAG);
                return;
            }

            NavHostFragment.findNavController(this)
                    .navigate(SummaryFragmentDirections
                            .actionNavigationPaymentSummaryToNavigationAccountSelection(transfer, paymentId));
        });

        mBinding.btnBackImageView.setOnClickListener(view12 ->
                mListener.onPaymentFailed(new TospayException("Payment canceled")));
    }

    private void showNetworkErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Connection Failed");
        builder.setMessage(getString(R.string.internet_error));
        builder.setPositiveButton("Retry", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            fetchPaymentDetails();
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            mListener.onPaymentFailed(new TospayException("Transaction canceled"));
        });
        builder.show();
    }

    private void fetchPaymentDetails() {
        if (NetworkUtils.isNetworkAvailable(getContext())) {
            mViewModel.details(paymentId);
            mViewModel.getDetailsResourceLiveData().observe(this, this::handleResponse);
        } else {
            showNetworkErrorDialog();
        }
    }

    private void handleResponse(Resource<Transfer> resource) {
        if (resource != null) {
            switch (resource.status) {
                case ERROR:
                    mViewModel.setIsLoading(false);
                    mViewModel.setIsError(true);
                    mViewModel.setErrorMessage(resource.message);
                    mListener.onPaymentFailed(new TospayException(resource.message));
                    break;

                case LOADING:
                    mViewModel.setIsLoading(true);
                    mViewModel.setIsError(false);
                    break;

                case SUCCESS:
                    mViewModel.setIsLoading(false);
                    mViewModel.setIsError(false);
                    transfer = resource.data;
                    mViewModel.getTransfer().setValue(transfer);
                    break;

                case RE_AUTHENTICATE:
                    mViewModel.setIsLoading(false);
                    mViewModel.setIsError(false);
                    startActivityForResult(new Intent(getContext(), AuthActivity.class), AuthActivity.REQUEST_CODE_LOGIN);
                    break;
            }
        }
    }

    @Override
    public int getBindingVariable() {
        return BR.summaryViewModel;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_summary;
    }

    @Override
    public SummaryViewModel getViewModel() {
        PaymentRepository repository = new PaymentRepository(getAppExecutors(),
                ServiceGenerator.createService(PaymentService.class, getContext()));
        PaymentViewModelFactory factory = new PaymentViewModelFactory(repository);
        mViewModel = ViewModelProviders.of(this, factory).get(SummaryViewModel.class);
        return mViewModel;
    }

    @Override
    public void onRefresh() {
        fetchPaymentDetails();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AuthActivity.REQUEST_CODE_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                if (mListener != null) {
                    mListener.onLoginSuccess(getSharedPrefManager().getActiveUser());
                }

                reloadBearerToken();
                NavHostFragment.findNavController(this)
                        .navigate(SummaryFragmentDirections
                                .actionNavigationPaymentSummaryToNavigationAccountSelection(transfer, paymentId));
            }
        }
    }

    @Override
    public void onLoginAsGuest() {

    }

    @Override
    public void onLoginTospayUser() {
        startActivityForResult(new Intent(getContext(), AuthActivity.class), AuthActivity.REQUEST_CODE_LOGIN);
    }
}

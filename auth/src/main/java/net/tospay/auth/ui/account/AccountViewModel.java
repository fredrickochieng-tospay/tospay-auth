package net.tospay.auth.ui.account;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.tospay.auth.interfaces.AccountType;
import net.tospay.auth.model.transfer.Transfer;
import net.tospay.auth.remote.Resource;
import net.tospay.auth.remote.repository.AccountRepository;
import net.tospay.auth.remote.repository.PaymentRepository;
import net.tospay.auth.ui.base.BaseViewModel;

import java.util.List;

public class AccountViewModel extends BaseViewModel<AccountNavigator>
        implements SwipeRefreshLayout.OnRefreshListener {

    private ObservableBoolean isEmpty;

    private AccountRepository accountRepository;
    private PaymentRepository paymentRepository;

    private LiveData<Resource<List<AccountType>>> accountsResourceLiveData;
    private LiveData<Resource<String>> paymentResourceLiveData;

    private MutableLiveData<Transfer> transfer;

    public AccountViewModel(AccountRepository accountRepository, PaymentRepository paymentRepository) {
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.isEmpty = new ObservableBoolean();
        this.transfer = new MutableLiveData<>();
    }

    public MutableLiveData<Transfer> getTransfer() {
        return transfer;
    }

    public LiveData<Resource<List<AccountType>>> getAccountsResourceLiveData() {
        return accountsResourceLiveData;
    }

    public LiveData<Resource<String>> getPaymentResourceLiveData() {
        return paymentResourceLiveData;
    }

    public void fetchAccounts(boolean showWallet) {
        String bearerToken = getBearerToken().get();
        accountsResourceLiveData = accountRepository.accounts(bearerToken, showWallet);
    }

    public void pay(String paymentId, Transfer transfer) {
        String bearerToken = getBearerToken().get();
        paymentResourceLiveData = paymentRepository.pay(bearerToken, paymentId, transfer);
    }

    @Override
    public void onRefresh() {
        getNavigator().onRefresh();
    }

    public ObservableBoolean getIsEmpty() {
        return isEmpty;
    }

    public void setIsEmpty(boolean isEmpty) {
        this.isEmpty.set(isEmpty);
    }
}

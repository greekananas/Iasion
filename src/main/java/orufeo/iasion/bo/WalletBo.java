package orufeo.iasion.bo;

import java.util.List;

import orufeo.iasion.data.objects.storage.Wallet;

public interface WalletBo {

	Wallet create(Wallet wallet);
	
	Wallet get(String guid);
	
	List<Wallet> get();
	
	void update(Wallet wallet);
	
	void update(Wallet wallet, String transactionId);
	
	void delete(String guid);
	
	List<Wallet> getForUser(String userGuid);
}

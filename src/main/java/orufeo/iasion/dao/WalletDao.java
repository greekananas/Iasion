package orufeo.iasion.dao;

import java.util.List;

import orufeo.iasion.data.objects.storage.Wallet;


public interface WalletDao {
	
	Wallet create(Wallet wallet);
	
	Wallet get(String guid);
	
	List<Wallet> get();
	
	void update(Wallet wallet);
	
	void delete(String guid);

}

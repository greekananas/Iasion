package orufeo.iasion.dao;

import java.util.List;

import orufeo.iasion.data.objects.storage.WalletHistory;

public interface WalletHistoryDao {
	
	WalletHistory create(WalletHistory walletHistory);
	
	List<WalletHistory> get(String walletGuid);
	
}

package orufeo.iasion.bo;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import lombok.Setter;
import orufeo.iasion.dao.UserAccountDao;
import orufeo.iasion.dao.WalletDao;
import orufeo.iasion.dao.WalletHistoryDao;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.Wallet;
import orufeo.iasion.data.objects.storage.WalletHistory;
import orufeo.iasion.data.objects.storage.WalletHistoryData;
import orufeo.iasion.data.objects.storage.WalletHistoryMetadata;
import orufeo.iasion.utils.CookiesValueTL;

public class WalletBoImpl implements WalletBo {

	@Setter private WalletDao walletDao;
	@Setter private WalletHistoryDao walletHistoryDao;
	@Setter private UserAccountDao userAccountDao;
	
	@Override
	public Wallet create(Wallet wallet) {
		
		return walletDao.create(wallet);
	}

	@Override
	public Wallet get(String guid) {
		
		return walletDao.get(guid);
	}

	@Override
	public List<Wallet> get() {
		
		return walletDao.get();
	}

	@Override
	public void update(Wallet wallet) {
		
		//********** History log
		Wallet previous = walletDao.get(wallet.getMetadata().getGuid());
		
		WalletHistory walletHistory = new WalletHistory();
		
		WalletHistoryMetadata metadata = new WalletHistoryMetadata();
		metadata.setCreationDate(new Date());
		metadata.setGuid(UUID.randomUUID().toString());
		
		String userToken = CookiesValueTL.get().get("token");
		UserAccount user = userAccountDao.getByToken(userToken);
				
		metadata.setAuthor(user.getData().getFirstname()+" "+user.getData().getLastname());
		metadata.setLastModifierGuid(user.getMetadata().getGuid());
		metadata.setModificationDate(new Date());
		
		WalletHistoryData data = new WalletHistoryData();
		data.setTime(new Date().getTime());
		data.setWalletData(previous.getData());
		data.setWalletGuid(previous.getMetadata().getGuid());
		
		walletHistory.setMetadata(metadata);
		walletHistory.setData(data);
		
		walletHistoryDao.create(walletHistory);
		
		//************* Update wallet
		
		wallet.getMetadata().setModificationDate(new Date());
		wallet.getMetadata().setLastModifierGuid(user.getMetadata().getGuid());
		walletDao.update(wallet);
		
	}

	@Override
	public void delete(String guid) {
		walletDao.delete(guid);
		
	}

}

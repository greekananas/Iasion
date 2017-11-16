package orufeo.iasion.component;

import java.util.List;
import java.util.Map;

import org.mule.api.annotations.param.Payload;

import lombok.Setter;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.bo.WalletBo;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.Wallet;
import orufeo.iasion.data.objects.storage.WalletMetadata;
import orufeo.iasion.utils.CookiesValueTL;

public class WalletSRV {

	@Setter private WalletBo walletBo;
	@Setter private UserAccountBo userAccountBo;

	public Wallet get(@Payload Map<String, String> args) {

		String guid = args.get("guid");

		return walletBo.get(guid);

	}

	public List<Wallet> getAll(@Payload Map<String, String> args) {

		return walletBo.get();

	}

	public Wallet create(@Payload Wallet wallet) {

		Map<String, String> cookies = CookiesValueTL.get();
		UserAccount user = userAccountBo.getByToken(cookies.get("token"));
		WalletMetadata metadata = new WalletMetadata(user);

		wallet.setMetadata(metadata);
		
		return walletBo.create(wallet);

	}

	public void update(@Payload Wallet wallet) {

		walletBo.update(wallet);

	}

	public void delete(@Payload Map<String, String> args) {

		String guid = args.get("guid");

		walletBo.delete(guid);

	}
	
}

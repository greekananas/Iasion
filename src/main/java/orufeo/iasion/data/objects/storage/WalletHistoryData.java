package orufeo.iasion.data.objects.storage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletHistoryData {

	private Long time;
	private String walletGuid;
	private String transactionId;
	private WalletData walletData;
	
}

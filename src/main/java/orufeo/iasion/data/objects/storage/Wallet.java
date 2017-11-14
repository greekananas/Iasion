package orufeo.iasion.data.objects.storage;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Wallet {

	private Integer id;
	private Date timestamp;
	private WalletMetadata metadata;
	private WalletData data;
	
}

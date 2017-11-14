package orufeo.iasion.data.objects.storage;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletHistory {

	private Integer id;
	private Date timestamp;
	private WalletHistoryMetadata metadata;
	private WalletHistoryData data;
	
}

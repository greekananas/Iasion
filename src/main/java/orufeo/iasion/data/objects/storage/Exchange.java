package orufeo.iasion.data.objects.storage;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Exchange {

	private Integer id;
	private Date timestamp;
	private ExchangeMetadata metadata;
	private ExchangeData data;
	
}

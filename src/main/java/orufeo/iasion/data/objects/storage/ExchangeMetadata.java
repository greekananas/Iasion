package orufeo.iasion.data.objects.storage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeMetadata extends Metadata {

	public ExchangeMetadata(UserAccount user ) {
		
		super(user);
	}
	
}

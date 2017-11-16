package orufeo.iasion.data.objects.storage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderMetadata extends Metadata {

	public OrderMetadata(UserAccount user ) {

		super(user);
	}
	
}

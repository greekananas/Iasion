package orufeo.iasion.data.objects.storage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAccountMetadata extends Metadata {

	public UserAccountMetadata(UserAccount user ) {

		super(user);
	}


}

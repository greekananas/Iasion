package orufeo.iasion.data.objects.storage;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAccount {
	
	private Integer id;
	private Date timestamp;
	private UserAccountMetadata metadata;
	private UserAccountData data;

}

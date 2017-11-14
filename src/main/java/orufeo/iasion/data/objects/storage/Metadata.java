package orufeo.iasion.data.objects.storage;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Metadata {
	
	private String guid;
	private Date creationDate;
	private Date modificationDate;
	private String author;
	private String lastModifierGuid;
	
	public Metadata(UserAccount user) {
		
		author = user.getData().getFirstname() + " " + user.getData().getLastname();
	    creationDate = new Date();
	    modificationDate = new Date();
	    lastModifierGuid = user.getMetadata().getGuid();
		
	}
	
	public Metadata() {
		
		author = "SYSTEM";
		creationDate = new Date();
		modificationDate = new Date();
		lastModifierGuid = "empty-guid";
		
	}

}

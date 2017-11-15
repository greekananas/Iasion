package orufeo.iasion.bo;

import java.util.List;

import orufeo.iasion.data.dto.Authentication;
import orufeo.iasion.data.objects.storage.UserAccount;

public interface UserAccountBo {
	
	Authentication authenticate(String login, String password);
	
	Authentication authenticate(String token);
	
	UserAccount create(UserAccount userAccount);
	
	UserAccount get(String guid);
	
	List<UserAccount> get();
	
	void update(UserAccount userAccount);
	
	void delete(String guid);

}

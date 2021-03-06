package orufeo.iasion.dao;

import java.util.List;

import orufeo.iasion.data.objects.storage.UserAccount;

public interface UserAccountDao {
	
	UserAccount create(UserAccount userAccount);
	
	UserAccount get(String guid);
	
	UserAccount getByLogin(String login);
	
	UserAccount getByToken(String token);
	
	List<UserAccount> get();
	
	void update(UserAccount userAccount);
	
	void delete(String guid);

}

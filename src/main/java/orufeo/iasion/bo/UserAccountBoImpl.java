package orufeo.iasion.bo;

import java.util.List;

import lombok.Setter;
import orufeo.iasion.dao.UserAccountDao;
import orufeo.iasion.dao.UserAccountDaoImpl;
import orufeo.iasion.data.dto.Authentication;
import orufeo.iasion.data.objects.storage.UserAccount;

public class UserAccountBoImpl implements UserAccountBo {

	@Setter private UserAccountDao userAccountDao;

	@Override
	public Authentication authenticate(String login, String password) {
		
		Authentication authentication = new Authentication();
		
		UserAccount user = userAccountDao.getByLogin(login);
		
		if (user != null) {

			if (user.getData().getPassword().equals(password) ) {
				
				authentication.setReason("OK");
				authentication.setId(user.getId());
				authentication.setLogin(user.getData().getLogin());
				authentication.setToken(user.getData().getToken());
				authentication.setRole(user.getData().getRole());
				authentication.setFirstname(user.getData().getFirstname());
				authentication.setLastname(user.getData().getLastname());

			} else {
				authentication.setReason("User deactivated");
				authentication.setId(-1);
				authentication.setLogin("Chuck.U.Farley");
				authentication.setAuthenticated(false);
				authentication.setToken("you_will_not_get_any_token_pal");
				authentication.setRole("Inspector Gadget");
				authentication.setFirstname("Chuck.U");
				authentication.setLastname("Farley");
			}
		}

		
		return authentication;
	}

	@Override
	public Authentication authenticate(String token) {
		
		Authentication authentication = new Authentication();

		UserAccount user = userAccountDao.getByToken(token);

		if (user != null) {

			if (user.getData().getToken().equals(token) ) {
				authentication.setReason("OK");
				authentication.setId(user.getId());
				authentication.setLogin(user.getData().getLogin());
				authentication.setToken(user.getData().getToken());
				authentication.setRole(user.getData().getRole());
				authentication.setFirstname(user.getData().getFirstname());
				authentication.setLastname(user.getData().getLastname());
			} else {
				authentication.setReason("User deactivated");
				authentication.setId(-1);
				authentication.setLogin("Chuck.U.Farley");
				authentication.setAuthenticated(false);
				authentication.setToken("you_will_not_get_any_token_pal");
				authentication.setRole("Inspector Gadget");
				authentication.setFirstname("Chuck.U");
				authentication.setLastname("Farley");
			}
		}

		return authentication;
	}

	@Override
	public UserAccount create(UserAccount userAccount) {
		
		return userAccountDao.create(userAccount);
	}

	@Override
	public UserAccount get(String guid) {
		
		return userAccountDao.get(guid);
	}

	@Override
	public List<UserAccount> get() {
		
		return userAccountDao.get();
	}

	@Override
	public void update(UserAccount userAccount) {
		
		userAccountDao.update(userAccount);
	}

	@Override
	public void delete(String guid) {
		
		userAccountDao.delete(guid);
		
	}

	@Override
	public UserAccount getByToken(String token) {
		
		return userAccountDao.getByToken(token);
	}

}

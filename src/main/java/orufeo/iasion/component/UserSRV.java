package orufeo.iasion.component;

import java.util.List;
import java.util.Map;

import org.mule.api.annotations.param.Payload;

import lombok.Setter;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.data.objects.storage.UserAccount;

public class UserSRV {

	@Setter private UserAccountBo userAccountBo;

	public UserAccount get(@Payload Map<String, String> args) {

		String guid = args.get("guid");

		return userAccountBo.get(guid);

	}

	public List<UserAccount> getAll(@Payload Map<String, String> args) {

		return userAccountBo.get();

	}

	public UserAccount create(@Payload UserAccount userAccount) {

		return userAccountBo.create(userAccount);

	}

	public void update(@Payload UserAccount userAccount) {

		userAccountBo.update(userAccount);

	}

	public void delete(@Payload Map<String, String> args) {

		String guid = args.get("guid");

		userAccountBo.delete(guid);

	}





}

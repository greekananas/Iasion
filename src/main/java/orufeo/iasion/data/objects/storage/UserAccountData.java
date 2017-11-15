package orufeo.iasion.data.objects.storage;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class UserAccountData {

		private String login;
		private String password;
		private String lastname;
		private String firstname;
		private String picUrl;
		private String token;
		private String role;
		private List<WalletData> wallets;
		
}

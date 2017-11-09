package orufeo.iasion.data.objects;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class User {

		private String login;
		private String password;
		private String lastname;
		private String firstname;
		private String picUrl;
		private String token;
		private List<Wallet> wallets;
		
}

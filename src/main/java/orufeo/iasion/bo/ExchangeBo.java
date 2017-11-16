package orufeo.iasion.bo;

import java.util.List;

import orufeo.iasion.data.objects.storage.Exchange;


public interface ExchangeBo {

	Exchange create(Exchange exchange);
	
	Exchange get(String guid);
	
	List<Exchange> get();
	
	void update(Exchange userAccount);
	
	void delete(String guid);
	
}

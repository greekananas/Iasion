package orufeo.iasion.data.objects.storage;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Order {

	private Integer id;
	private Date timestamp;
	private OrderMetadata metadata;
	private OrderData data;
}

package orufeo.iasion.data.dto;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import orufeo.iasion.data.objects.ConversionType;
import orufeo.iasion.data.objects.Ohclv;

@Getter
@Setter
public class CryptoPair {

	@JsonProperty("Response")
	private String response ;
	
	@JsonProperty("Message")
	private String message ;
	
	@JsonProperty("Type")
	private Integer type;
	
	@JsonProperty("Aggregated")
	private Boolean aggregated;
	
	@JsonProperty("Data")
	private List<Ohclv> data;
	
	@JsonProperty("TimeTo")
	private Long timeTo;
	
	@JsonProperty("TimeFrom")
	private Long  timeFrom;
	
	@JsonProperty("FirstValueInArray")
	private Boolean firstValueInArray;
	
	@JsonProperty("ConversionType")
	private ConversionType conversionType;
	
}

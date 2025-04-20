package sovok.mcbuildlibrary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.validation.NotPurelyNumeric;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateRequestDto {

    @Valid // Ensures validation annotations within NameDto are checked
    @Size(max = 1000, message = "Author list cannot exceed 1000 entries")
    private List<NameDto> authors;

    @Valid
    @Size(max = 1000, message = "Theme list cannot exceed 1000 entries")
    private List<NameDto> themes;

    @Valid
    @Size(max = 1000, message = "Color list cannot exceed 1000 entries")
    private List<NameDto> colors;

    // Inner record for name validation
    public record NameDto(
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE) // Consistent min size
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            String name
    ) {}
}
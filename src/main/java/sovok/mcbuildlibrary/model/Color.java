// file: src/main/java/sovok/mcbuildlibrary/model/Color.java
package sovok.mcbuildlibrary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank; // Import
import jakarta.validation.constraints.Size;     // Import
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sovok.mcbuildlibrary.exception.StringConstants; // Import

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = StringConstants.NAME_NOT_BLANK)
    // Remove max = 30, keep min = 2 if desired
    @Size(min = 2, message = StringConstants.NAME_SIZE)
    @Column(unique = true, nullable = false)
    private String name;
}
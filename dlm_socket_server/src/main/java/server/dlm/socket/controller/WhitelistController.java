package server.dlm.socket.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.dlm.socket.dto.ResponseData;
import server.dlm.socket.entity.Whitelist;
import server.dlm.socket.repository.WhitelistRepository;

import java.util.List;

@RestController
@Log4j2
@RequestMapping("/api/whitelist")
public class WhitelistController {

    private final WhitelistRepository whitelistRepository;

    public WhitelistController(WhitelistRepository whitelistRepository) {
        this.whitelistRepository = whitelistRepository;
    }

    @Operation(summary = "Import IMEI list to whitelist")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Request body is a list IMEI, example : [\n" +
                            "  \"352840051234567\",\n" +
                            "  \"352840051234568\"\n" +
                            "] ",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 0,
                                      "message": "Success!",
                                      "timeResponse": "2025-04-07 16:09:02.325",
                                      "uuid": null,
                                      "path": null,
                                      "data": "IMEI list imported successfully"
                                    }
                                    """))
            )
    })
    @PostMapping("/import")
    public ResponseData<?> importWhitelist(@RequestBody List<String> imeiList) {
        imeiList.forEach(imei -> {
            try {
                if (!whitelistRepository.existsByImei(imei)) whitelistRepository.save(new Whitelist(imei));
            } catch (Exception e) {
                log.error("Import Whitelist error: {}", String.valueOf(e));
            }
        });
        return new ResponseData<>().success("IMEI list imported successfully");
    }

    @Operation(summary = "Get whitelist IMEIs")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of whitelisted IMEIs",
                    content = @Content(array = @ArraySchema(schema = @Schema(example = "352840051234567")))
            )
    })
    @GetMapping("/list")
    public ResponseEntity<List<String>> getWhitelist() {
        List<String> imeis = whitelistRepository.findAll().stream().map(Whitelist::getImei).toList();
        return ResponseEntity.ok(imeis);
    }

    @Operation(summary = "Check if IMEI is whitelisted")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "True if IMEI exists in whitelist, false otherwise",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "true"))
            )
    })
    @GetMapping("/check/{imei}")
    public ResponseEntity<Boolean> checkWhitelist(@PathVariable String imei) {
        boolean exists = whitelistRepository.existsByImei(imei);
        return ResponseEntity.ok(exists);
    }
}

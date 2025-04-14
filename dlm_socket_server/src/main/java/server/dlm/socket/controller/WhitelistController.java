package server.dlm.socket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.web.bind.annotation.*;
import server.dlm.socket.dto.ResponseData;
import server.dlm.socket.entity.main.Whitelist;
import server.dlm.socket.repository.main.WhitelistRepository;

import java.util.List;

@RestController
@Log4j2
@RequestMapping("/api/whitelist")
@Description("Show API details in http://localhost:9000/swagger-ui/index.html#/")
public class WhitelistController {

    @Autowired
    private WhitelistRepository whitelistRepository;

    @Operation(summary = "Import IMEI list to whitelist")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Import a list of IMEI, e.g: [\"352840051234567\",\"352840051234568\"] ",
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
    public ResponseData<?> getWhitelist() {
        List<String> imeiList = whitelistRepository.findAll().stream().map(Whitelist::getImei).toList();
        return new ResponseData<>().success(imeiList);
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
    public ResponseData<?> checkWhitelist(@PathVariable String imei) {
        boolean exists = whitelistRepository.existsByImei(imei);
        return new ResponseData<>().success(exists);
    }
}

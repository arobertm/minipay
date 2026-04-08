package ro.minipay.minids.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.minids.model.Entry;

import java.util.List;
import java.util.Map;

/**
 * REST API for MiniDS — inspired by the PingDS REST API.
 *
 * PingDS real exposes:
 *   GET    /users/{id}
 *   POST   /users
 *   PUT    /users/{id}
 *   DELETE /users/{id}
 *   POST   /users?_action=search
 *
 * MiniDS exposes the same concept but with an explicit DN in the path:
 *   GET    /minids/v1/entries/{dn}
 *   POST   /minids/v1/entries
 *   PUT    /minids/v1/entries/{dn}
 *   DELETE /minids/v1/entries/{dn}
 *   POST   /minids/v1/search
 *
 * All writes (POST/PUT/DELETE) go through Raft.
 * Reads (GET/search) are local (eventual consistency).
 */
@Slf4j
@RestController
@RequestMapping("/minids/v1")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    // ─── CREATE ───────────────────────────────────────────────

    /**
     * Creates a new entry in the directory.
     *
     * Example request:
     * POST /minids/v1/entries
     * {
     *   "dn": "uid=john,ou=users,dc=minipay,dc=ro",
     *   "objectClass": "minipayUser",
     *   "uid": "john",
     *   "cn": "John Doe",
     *   "mail": "john@example.com"
     * }
     */
    @PostMapping("/entries")
    public ResponseEntity<Entry> createEntry(@RequestBody Entry entry) {
        if (directoryService.exists(entry.getDn())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Entry created = directoryService.createEntry(entry);
        log.info("Entry created: {}", created.getDn());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── READ ─────────────────────────────────────────────────

    /**
     * Reads an entry by DN.
     *
     * Example: GET /minids/v1/entries/uid=john,ou=users,dc=minipay,dc=ro
     */
    @GetMapping("/entries/{dn}")
    public ResponseEntity<Entry> getEntry(@PathVariable String dn) {
        return directoryService.getEntry(dn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── UPDATE ───────────────────────────────────────────────

    /**
     * Replaces an existing entry (PUT = full replace).
     */
    @PutMapping("/entries/{dn}")
    public ResponseEntity<Entry> updateEntry(
            @PathVariable String dn,
            @RequestBody Entry entry) {

        if (!directoryService.exists(dn)) {
            return ResponseEntity.notFound().build();
        }
        Entry updated = directoryService.updateEntry(dn, entry);
        return ResponseEntity.ok(updated);
    }

    /**
     * Updates only the submitted attributes (PATCH = partial merge).
     */
    @PatchMapping("/entries/{dn}")
    public ResponseEntity<Entry> patchEntry(
            @PathVariable String dn,
            @RequestBody Map<String, Object> attributes) {

        return directoryService.getEntry(dn)
                .map(existing -> {
                    attributes.forEach(existing::setAttribute);
                    return ResponseEntity.ok(directoryService.updateEntry(dn, existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── DELETE ───────────────────────────────────────────────

    /**
     * Deletes an entry from the directory.
     */
    @DeleteMapping("/entries/{dn}")
    public ResponseEntity<Void> deleteEntry(@PathVariable String dn) {
        if (!directoryService.exists(dn)) {
            return ResponseEntity.notFound().build();
        }
        directoryService.deleteEntry(dn);
        log.info("Entry deleted: {}", dn);
        return ResponseEntity.noContent().build();
    }

    // ─── SEARCH ───────────────────────────────────────────────

    /**
     * Searches for entries under a baseDN with a filter.
     * Inspired by LDAP Search + PingDS ?_queryFilter=
     *
     * Example request:
     * POST /minids/v1/search
     * {
     *   "baseDn": "ou=users,dc=minipay,dc=ro",
     *   "filter": { "accountStatus": "active" },
     *   "limit": 50
     * }
     */
    @PostMapping("/search")
    public ResponseEntity<List<Entry>> search(@RequestBody SearchRequest request) {
        List<Entry> results = directoryService.search(
                request.baseDn(),
                request.filter(),
                request.limit()
        );
        return ResponseEntity.ok(results);
    }

    // ─── HEALTH ───────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "minids"
        ));
    }
}

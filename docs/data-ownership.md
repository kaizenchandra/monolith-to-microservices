# Authoritative datasets

| Tables                     | Initial writer              | Final writer                                     | Readers / replication                                    | Retirement                                               |
|----------------------------|-----------------------------|--------------------------------------------------|----------------------------------------------------------|----------------------------------------------------------|
| legacy.products            | legacy                      | Catalog products DB                              | gateway/checkout; fenced copy, versioned snapshot replay | retain frozen for reverse copy; do not drop yet          |
| legacy.stock, reservations | legacy                      | Inventory DB                                     | Saga; full copy includes reservation identity/state      | retain fenced until workflow/rollback retention approval |
| legacy.orders, payments    | legacy historical           | legacy historical, immutable after checkout gate | API/reporting                                            | archive only after legal retention review                |
| checkout                   | none                        | Orders                                           | API + worker                                             | active                                                   |
| authorizations             | legacy simulator historical | Payment for new order IDs                        | Saga only                                                | active                                                   |
| customers                  | legacy                      | legacy                                           | Orders HTTP resolver                                     | no retirement                                            |
| shipments, notifications   | legacy                      | legacy                                           | transactional inbox consumer                             | no retirement                                            |
| modern_orders              | none                        | legacy consumer                                  | combined reporting                                       | rebuildable from retained events                         |
| ownership, routing         | operator/admin              | operator/admin                                   | writer fences / gateway                                  | persistent control dependency                            |
| outbox                     | service transaction         | same service                                     | relay only mutates publish status                        | purge only after audited retention                       |
| inbox                      | consumer                    | consumer                                         | duplicate suppression                                    | retain beyond replay horizon                             |

Source `MODERN` means delegate, target `MODERN` means active. Source `FROZEN` rejects writes. Target starts FROZEN.
`epoch` is a compare-and-set administrative token; ownership states persist through restart. An operator with admin
credentials can violate the protocol: administrative access must be serialized and audited. Do not bypass
migration-tools or activate both databases independently.

Reconciliation reports are point-in-time cutover evidence. After modern writes begin, frozen legacy backup rows are
expected to diverge. Do not treat those rows as an authoritative oracle or serve them during reverse copy; reconcile
after freezing and synchronizing the current authority.

# Architecture

SMS
 ↓
MpesaSmsReceiver / HistoricalSmsImporter
 ↓
MpesaParser
 ↓
ParsedTransaction
 ↓
Validation
 ↓
TransactionRepository
 ↓
Room / SQLite
 ↓
Analytics repository
 ↓
Jetpack Compose UI

The important principle is that SMS parsing is independent from UI.

This allows the parser to be tested independently and later reused for batch imports.

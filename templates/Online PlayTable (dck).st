// Deck file for Online PlayTable (http://www.onlineplaytable.com)

$deckCards:{card|    $card.qty$ [$card.shortSet$] $card.nameWithPictureNumber$}; separator="\n"$

// Sideboard
$sideCards:{card|SB: $card.qty$ [$card.shortSet$] $card.nameWithPictureNumber$}; separator="\n"$

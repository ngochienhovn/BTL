# Socket Endpoint Specification

## 1) LOGIN
- Request `type=LOGIN`
- Required fields: `email`, `password`
- Response `type=LOGIN_RESULT`
- Success payload (`data`): `id`, `fullName`, `email`, `role`
- Failure: `success=false`, `code` in (`USER_NOT_FOUND`, `INVALID_PASSWORD`)

## 2) REGISTER
- Request `type=REGISTER`
- Required fields: `fullName`, `email`, `password`, `role` (`BIDDER|SELLER|ADMIN`)
- Response `type=REGISTER_RESULT`
- Success payload (`data`): `id`, `fullName`, `email`, `role`
- Failure: `success=false`, `code=EMAIL_EXISTS`

## 3) CREATE_ITEM
- Request `type=CREATE_ITEM`
- Required fields: `userId`, `email` (or `sellerEmail`), `role`, `typeOfItem`, `itemName`, `itemStartingBid`
- Optional fields: `itemDescription`, `imageUrl`
- Response `type=CREATE_ITEM_RESULT`
- Success payload (`data`): `itemId`
- Failure: `success=false`, `code` in (`FORBIDDEN`, `CREATE_ITEM_FAILED`)

## 4) UPDATE_ITEM
- Request `type=UPDATE_ITEM`
- Required fields: `userId`, `email` (or `sellerEmail`), `role`, `itemId`, `typeOfItem`, `itemName`, `itemStartingBid`
- Optional fields: `itemDescription`, `imageUrl`
- Response `type=UPDATE_ITEM_RESULT`
- Failure: `success=false`, `code` in (`FORBIDDEN`, `NOT_FOUND`)

## 5) DELETE_ITEM
- Request `type=DELETE_ITEM`
- Required fields: `userId`, `email` (or `sellerEmail`), `role`, `itemId`
- Response `type=DELETE_ITEM_RESULT`
- Failure: `success=false`, `code` in (`FORBIDDEN`, `NOT_FOUND`)

## 6) GET_ITEMS_BY_SELLER
- Request `type=GET_ITEMS_BY_SELLER`
- Required fields: `userId`, `email`, `role`
- Response `type=GET_ITEMS_BY_SELLER_RESULT`
- Success payload (`items`): list of item objects with:
  - `id`, `sellerId`, `sellerEmail`, `type`, `name`, `description`, `startingBid`, `imageUrl`

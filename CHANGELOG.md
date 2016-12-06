### 1.2.0 (Dec 6, 2016)

- New features
  - Support for Scala 2.12;
  - New method `closeAll` in `HtmlUnitBrowser`, for closing opened windows;
  - New model `Node` representing a DOM node - in this library, either a `ElementNode` or a `TextNode`;
  - New methods `childNodes` and `siblingNodes` in `Element`.

### 1.1.0 (Sep 25, 2016)

- New features
  - New methods `clearCookies`, `parseInputStream` and `parseResource` in `Browser`;
  - New methods `hasAttr` and `siblings` in `Element`;
  - Support for SOCKS proxies.

- Bug fixes
  - Correct handling of missing name and value attributes in the `formData` extractor.

### 1.0.0 (Apr 5, 2016)

First stable version.

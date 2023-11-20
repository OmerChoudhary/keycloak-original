// tslint:disable:no-unused-expression
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Server Info", () => {
  let client: KeycloakAdminClient;

  before(async () => {
    client = new KeycloakAdminClient();
    await client.auth(credentials);
  });

  it("list server info", async () => {
    const serverInfo = await client.serverInfo.find();
    expect(serverInfo).to.be.ok;
  });

  it("list effective message bundles of a realm", async () => {
    const messageBundles = await client.serverInfo.findEffectiveMessageBundles({
      realm: "master",
    });

    expect(messageBundles).to.be.ok;
  });
});

package com.mobilesample;

import java.io.IOException;

import javax.inject.Named;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.response.CollectionResponse;

/**
 * A simple Cloud Endpoint that receives notifications from a web client
 * (<server url>/index.html), and broadcasts them to all of the devices that
 * were registered with this application (via DeviceInfoEndpoint).
 * 
 * In order for this sample to work, you have to populate the API_KEY field with
 * your key for server apps. You can obtain this key from the API console
 * (https://code.google.com/apis/console). You'll first have to create a project
 * and enable the Google Cloud Messaging Service for it, as described in the
 * javadoc for GCMIntentService.java (in your Android project).
 * 
 * After creating the project in the API console, browse to the "API Access"
 * section, and select the option to "Create a New Server Key". The generated
 * key is what you'll enter into the API_KEY field.
 * 
 * See the documentation at
 * http://developers.google.com/eclipse/docs/cloud_endpoints for more
 * information.
 * 
 * NOTE: This endpoint does not use any form of authorization or authentication!
 * If this app is deployed, anyone can access this endpoint! If you'd like to
 * add authentication, take a look at the documentation.
 */
@Api(name = "messageEndpoint")
// NO AUTHENTICATION; OPEN ENDPOINT!
public class MessageEndpoint {

  /*
   * TODO: Fill this in with the server key that you've obtained from the API
   * Console (https://code.google.com/apis/console).
   */
  private static final String API_KEY = "AIzaSyC-GyL1xOYOvo7VniZObPDghxajPMZmFRc";

  private static final DeviceInfoEndpoint endpoint = new DeviceInfoEndpoint();

  /**
   * This inserts a new entity into App Engine datastore. If the entity
   * already exists in the datastore, an exception is thrown. It uses HTTP
   * POST method.
   * 
   * @param gcmmessage
   *            the entity to be inserted.
   * @return The inserted entity.
   * @throws IOException
   */
  public void sendMessage(@Named("message") String message)
      throws IOException {
    Sender sender = new Sender(API_KEY);
    // ping a max of 10 registered devices
    CollectionResponse<DeviceInfo> response = endpoint.listDeviceInfo(null,
        10);
    for (DeviceInfo deviceInfo : response.getItems()) {
      doSendViaGcm(message, sender, deviceInfo);
    }
  }

  /**
   * Sends the message using the Sender object to the registered device.
   * 
   * @param message
   *            the message to be sent in the GCM ping to the device.
   * @param sender
   *            the Sender object to be used for ping,
   * @param deviceInfo
   *            the registration id of the device.
   * @return Result the result of the ping.
   */
  private static Result doSendViaGcm(String message, Sender sender,
      DeviceInfo deviceInfo) throws IOException {
    // Trim message if needed.
    if (message.length() > 1000) {
      message = message.substring(0, 1000) + "[...]";
    }

    Message msg = new Message.Builder().addData("message", message).build();
    Result result = sender.send(msg, deviceInfo.getDeviceRegistrationID(),
        5);
    if (result.getMessageId() != null) {
      String canonicalRegId = result.getCanonicalRegistrationId();
      if (canonicalRegId != null) {
        endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
        deviceInfo.setDeviceRegistrationID(canonicalRegId);
        endpoint.insertDeviceInfo(deviceInfo);
      }
    } else {
      String error = result.getErrorCodeName();
      if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
        endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
      }
    }

    return result;
  }

}

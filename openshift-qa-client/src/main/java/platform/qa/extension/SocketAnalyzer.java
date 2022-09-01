/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.qa.extension;

import lombok.SneakyThrows;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Provides available port for TCP connection.
 * Example:
 *  <p>
 *      {@code
 *          int availablePort = new SocketAnalyzer().getAvailablePort();
 *      }
 *  </p>
 */
public class SocketAnalyzer extends ServerSocket {


    public SocketAnalyzer() throws IOException{
    }

    /**
     * Provides available port for TCP connection
     * @return available port that can be used for TCP connection
     */
    @SneakyThrows(IOException.class)
    public int getAvailablePort() {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
    }
}

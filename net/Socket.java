/*
 * @(#)Socket.java	1.97 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * This class implements client sockets (also called just
 * "sockets"). A socket is an endpoint for communication
 * between two machines.
 * <p>
 * The actual work of the socket is performed by an instance of the
 * <code>SocketImpl</code> class. An application, by changing
 * the socket factory that creates the socket implementation,
 * can configure itself to create sockets appropriate to the local
 * firewall.
 *
 * @author  unascribed
 * @version 1.97, 01/23/03
 * @see     java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
 * @see     java.net.SocketImpl
 * @see     java.nio.channels.SocketChannel
 * @since   JDK1.0
 */
public
class Socket {
    /**
     * Various states of this socket.
     */
    private boolean created = false;
    private boolean bound = false;
    private boolean connected = false;
    private boolean closed = false;
    private Object closeLock = new Object();
    private boolean shutIn = false;
    private boolean shutOut = false;

    /**
     * The implementation of this Socket.
     */
    SocketImpl impl;

    /**
     * Are we using an older SocketImpl?
     */
    private boolean oldImpl = false;

    /**
     * Creates an unconnected socket, with the
     * system-default type of SocketImpl.
     *
     * @since   JDK1.1
     * @revised 1.4
     */
    public Socket() {
	setImpl();
    }

    /**
     * Creates an unconnected Socket with a user-specified
     * SocketImpl.
     * <P>
     * @param impl an instance of a <B>SocketImpl</B>
     * the subclass wishes to use on the Socket.
     *
     * @exception SocketException if there is an error in the underlying protocol,     
     * such as a TCP error. 
     * @since   JDK1.1
     */
    protected Socket(SocketImpl impl) throws SocketException {
	this.impl = impl;
	if (impl != null) {
	    checkOldImpl();
	    this.impl.setSocket(this);
	}
    }

    /**
     * Creates a stream socket and connects it to the specified port
     * number on the named host.
     * <p>
     * If the specified host is <tt>null</tt> it is the equivalent of
     * specifying the address as <tt>{@link java.net.InetAddress#getByName InetAddress.getByName}(null)</tt>.
     * In other words, it is equivalent to specifying an address of the 
     * loopback interface. </p>
     * <p>
     * If the application has specified a server socket factory, that
     * factory's <code>createSocketImpl</code> method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     *
     * @param      host   the host name, or <code>null</code> for the loopback address.
     * @param      port   the port number.
     *
     * @exception  UnknownHostException if the IP address of 
     * the host could not be determined.
     *
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        SecurityManager#checkConnect
     */
    public Socket(String host, int port)
	throws UnknownHostException, IOException
    {
	this(host != null ? new InetSocketAddress(host, port) :
	     new InetSocketAddress(InetAddress.getByName(null), port),
	     new InetSocketAddress(0), true);
    }

    /**
     * Creates a stream socket and connects it to the specified port
     * number at the specified IP address.
     * <p>
     * If the application has specified a socket factory, that factory's
     * <code>createSocketImpl</code> method is called to create the
     * actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     * 
     * @param      address   the IP address.
     * @param      port      the port number.
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        SecurityManager#checkConnect
     */
    public Socket(InetAddress address, int port) throws IOException {
	this(address != null ? new InetSocketAddress(address, port) : null, 
	     new InetSocketAddress(0), true);
    }

    /**
     * Creates a socket and connects it to the specified remote host on
     * the specified remote port. The Socket will also bind() to the local
     * address and port supplied.
     * <p>
     * If the specified host is <tt>null</tt> it is the equivalent of
     * specifying the address as <tt>{@link java.net.InetAddress#getByName InetAddress.getByName}(null)</tt>.
     * In other words, it is equivalent to specifying an address of the 
     * loopback interface. </p>
     * <p>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     * 
     * @param host the name of the remote host, or <code>null</code> for the loopback address.
     * @param port the remote port
     * @param localAddr the local address the socket is bound to
     * @param localPort the local port the socket is bound to
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        SecurityManager#checkConnect
     * @since   JDK1.1
     */
    public Socket(String host, int port, InetAddress localAddr,
		  int localPort) throws IOException {
	this(host != null ? new InetSocketAddress(host, port) :
	       new InetSocketAddress(InetAddress.getByName(null), port),
	     new InetSocketAddress(localAddr, localPort), true);
    }

    /**
     * Creates a socket and connects it to the specified remote address on
     * the specified remote port. The Socket will also bind() to the local
     * address and port supplied.
     * <p>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     * 
     * @param address the remote address
     * @param port the remote port
     * @param localAddr the local address the socket is bound to
     * @param localPort the local port the socket is bound to
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        SecurityManager#checkConnect
     * @since   JDK1.1
     */
    public Socket(InetAddress address, int port, InetAddress localAddr,
		  int localPort) throws IOException {
	this(address != null ? new InetSocketAddress(address, port) : null,
	     new InetSocketAddress(localAddr, localPort), true);
    }

    /**
     * Creates a stream socket and connects it to the specified port
     * number on the named host.
     * <p>
     * If the specified host is <tt>null</tt> it is the equivalent of
     * specifying the address as <tt>{@link java.net.InetAddress#getByName InetAddress.getByName}(null)</tt>.
     * In other words, it is equivalent to specifying an address of the 
     * loopback interface. </p>
     * <p>
     * If the stream argument is <code>true</code>, this creates a
     * stream socket. If the stream argument is <code>false</code>, it
     * creates a datagram socket.
     * <p>
     * If the application has specified a server socket factory, that
     * factory's <code>createSocketImpl</code> method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     * <p>
     * If a UDP socket is used, TCP/IP related socket options will not apply.
     *
     * @param      host     the host name, or <code>null</code> for the loopback address.
     * @param      port     the port number.
     * @param      stream   a <code>boolean</code> indicating whether this is
     *                      a stream socket or a datagram socket.
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        SecurityManager#checkConnect
     * @deprecated Use DatagramSocket instead for UDP transport.
     */
    public Socket(String host, int port, boolean stream) throws IOException {
	this(host != null ? new InetSocketAddress(host, port) :
	       new InetSocketAddress(InetAddress.getByName(null), port),
	     new InetSocketAddress(0), stream);
    }

    /**
     * Creates a socket and connects it to the specified port number at
     * the specified IP address.
     * <p>
     * If the stream argument is <code>true</code>, this creates a
     * stream socket. If the stream argument is <code>false</code>, it
     * creates a datagram socket.
     * <p>
     * If the application has specified a server socket factory, that
     * factory's <code>createSocketImpl</code> method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * 
     * <p>If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with <code>host.getHostAddress()</code> and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     * <p>
     * If UDP socket is used, TCP/IP related socket options will not apply.
     *
     * @param      host     the IP address.
     * @param      port      the port number.
     * @param      stream    if <code>true</code>, create a stream socket;
     *                       otherwise, create a datagram socket.
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        SecurityManager#checkConnect
     * @deprecated Use DatagramSocket instead for UDP transport.
     */
    public Socket(InetAddress host, int port, boolean stream) throws IOException {
	this(host != null ? new InetSocketAddress(host, port) : null, 
	     new InetSocketAddress(0), stream);
    }

    private Socket(SocketAddress address, SocketAddress localAddr,
		   boolean stream) throws IOException {
	setImpl();

	// backward compatibility
	if (address == null)
	    throw new NullPointerException();

	try {
	    createImpl(stream);
	    if (localAddr == null)
		localAddr = new InetSocketAddress(0);
	    bind(localAddr);
	    if (address != null)
		connect(address);
	} catch (SocketException e) {
	    close();
	    throw e;
	}
    }

    /**
     * Creates the socket implementation.
     *
     * @param stream a <code>boolean</code> value : <code>true</code> for a TCP socket,
     *		     <code>false</code> for UDP.
     * @throws IOException if creation fails
     * @since 1.4
     */
     void createImpl(boolean stream) throws SocketException {
	if (impl == null) 
	    setImpl();
	try {
	    impl.create(stream);
	    created = true;
	} catch (IOException e) {
	    throw new SocketException(e.getMessage());
	}
    }

    private void checkOldImpl() {
	if (impl == null)
	    return;
	// SocketImpl.connect() is a protected method, therefore we need to use
	// getDeclaredMethod, therefore we need permission to access the member
	try {
	    AccessController.doPrivileged(new PrivilegedExceptionAction() {
		    public Object run() throws NoSuchMethodException {
			Class[] cl = new Class[2];
			cl[0] = SocketAddress.class;
			cl[1] = Integer.TYPE;
			impl.getClass().getDeclaredMethod("connect", cl);
			return null;
		    }
		});
	} catch (java.security.PrivilegedActionException e) {
	    oldImpl = true;
	}
    }

    /**
     * Sets impl to the system-default type of SocketImpl.
     * @since 1.4
     */
    void setImpl() {
	checkSocks();
	if (factory != null) {
	    impl = factory.createSocketImpl();
	    checkOldImpl();
	} else {
	    // No need to do a checkOldImpl() here, we know it's an up to date
	    // SocketImpl!
	    impl = new PlainSocketImpl();
	}
	if (impl != null)
	    impl.setSocket(this);
    }


    /**
     * Get the <code>SocketImpl</code> attached to this socket, creating
     * it if necessary.
     *
     * @return	the <code>SocketImpl</code> attached to that ServerSocket.
     * @throws SocketException if creation fails
     * @since 1.4
     */
    SocketImpl getImpl() throws SocketException {
	if (!created)
	    createImpl(true);
	return impl;
    }

    /**
     * Connects this socket to the server.
     *
     * @param	endpoint the <code>SocketAddress</code>
     * @throws	IOException if an error occurs during the connection
     * @throws  java.nio.channels.IllegalBlockingModeException
     *          if this socket has an associated channel,
     *          and the channel is in non-blocking mode
     * @throws  IllegalArgumentException if endpoint is null or is a
     *          SocketAddress subclass not supported by this socket
     * @since 1.4
     * @spec JSR-51
     */
    public void connect(SocketAddress endpoint) throws IOException {
	connect(endpoint, 0);
    }

    /**
     * Connects this socket to the server with a specified timeout value.
     * A timeout of zero is interpreted as an infinite timeout. The connection
     * will then block until established or an error occurs.
     *
     * @param	endpoint the <code>SocketAddress</code>
     * @param	timeout  the timeout value to be used in milliseconds.
     * @throws	IOException if an error occurs during the connection
     * @throws	SocketTimeoutException if timeout expires before connecting
     * @throws  java.nio.channels.IllegalBlockingModeException
     *          if this socket has an associated channel,
     *          and the channel is in non-blocking mode
     * @throws  IllegalArgumentException if endpoint is null or is a
     *          SocketAddress subclass not supported by this socket
     * @since 1.4
     * @spec JSR-51
     */
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
	if (endpoint == null)
	    throw new IllegalArgumentException("connect: The address can't be null");

	if (timeout < 0)
	  throw new IllegalArgumentException("connect: timeout can't be negative");

	if (isClosed())
	    throw new SocketException("Socket is closed");

	if (!oldImpl && isConnected())
	    throw new SocketException("already connected");

	if (!(endpoint instanceof InetSocketAddress))
	    throw new IllegalArgumentException("Unsupported address type");

	InetSocketAddress epoint = (InetSocketAddress) endpoint;

	SecurityManager security = System.getSecurityManager();
	if (security != null) {
	    if (epoint.isUnresolved())
		security.checkConnect(epoint.getHostName(),
				      epoint.getPort());
	    else
		security.checkConnect(epoint.getAddress().getHostAddress(),
				      epoint.getPort());
	}
	if (!created)
	    createImpl(true);
	if (!oldImpl)
	    impl.connect(epoint, timeout);
	else if (timeout == 0) {
	    if (epoint.isUnresolved())
		impl.connect(epoint.getAddress().getHostName(),
			     epoint.getPort());
	    else
		impl.connect(epoint.getAddress(), epoint.getPort());
	} else
	    throw new UnsupportedOperationException("SocketImpl.connect(addr, timeout)");
	connected = true;
	/*
	 * If the socket was not bound before the connect, it is now because
	 * the kernel will have picked an ephemeral port & a local address
	 */
	bound = true;
    }

    /**
     * Binds the socket to a local address.
     * <P>
     * If the address is <code>null</code>, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     *
     * @param	bindpoint the <code>SocketAddress</code> to bind to
     * @throws	IOException if the bind operation fails, or if the socket
     *			   is already bound.
     * @throws  IllegalArgumentException if bindpoint is a
     *          SocketAddress subclass not supported by this socket
     *
     * @since	1.4
     * @see #isBound
     */
    public void bind(SocketAddress bindpoint) throws IOException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	if (!oldImpl && isBound())
	    throw new SocketException("Already bound");

	if (bindpoint != null && (!(bindpoint instanceof InetSocketAddress)))
	    throw new IllegalArgumentException("Unsupported address type");
	InetSocketAddress epoint = (InetSocketAddress) bindpoint;
	if (epoint != null && epoint.isUnresolved())
	    throw new SocketException("Unresolved address");
	if (bindpoint == null)
	    getImpl().bind(InetAddress.anyLocalAddress(), 0);
	else
	    getImpl().bind(epoint.getAddress(),
			   epoint.getPort());
	bound = true;
    }

    /**
     * set the flags after an accept() call.
     */
    final void postAccept() { 
	connected = true;
	created = true;
	bound = true;
    }

    void setCreated() {
	created = true;
    }

    void setBound() {
	bound = true;
    }

    void setConnected() {
	connected = true;
    }

    /**
     * Returns the address to which the socket is connected.
     *
     * @return  the remote IP address to which this socket is connected,
     *		or <code>null</code> if the socket is not connected.
     */
    public InetAddress getInetAddress() {
	if (!isConnected())
	    return null;
	try {
	    return getImpl().getInetAddress();
	} catch (SocketException e) {
	}
	return null;
    }

    /**
     * Gets the local address to which the socket is bound.
     *
     * @return the local address to which the socket is bound or 
     *	       <code>InetAddress.anyLocalAddress()</code>
     *	       if the socket is not bound yet.
     * @since   JDK1.1
     */
    public InetAddress getLocalAddress() {
	// This is for backward compatibility
	if (!isBound())
	    return InetAddress.anyLocalAddress();
	InetAddress in = null;
	try {
	    in = (InetAddress) getImpl().getOption(SocketOptions.SO_BINDADDR);
	    if (in.isAnyLocalAddress()) {
		in = InetAddress.anyLocalAddress();
	    }
	} catch (Exception e) {
	    in = InetAddress.anyLocalAddress(); // "0.0.0.0"
	}
	return in;
    }

    /**
     * Returns the remote port to which this socket is connected.
     *
     * @return  the remote port number to which this socket is connected, or
     *	        0 if the socket is not connected yet.
     */
    public int getPort() {
	if (!isConnected())
	    return 0;
	try {
	    return getImpl().getPort();
	} catch (SocketException e) {
	    // Shouldn't happen as we're connected
	}
	return -1;
    }

    /**
     * Returns the local port to which this socket is bound.
     *
     * @return  the local port number to which this socket is bound or -1
     *	        if the socket is not bound yet.
     */
    public int getLocalPort() {
	if (!isBound())
	    return -1;
	try {
	    return getImpl().getLocalPort();
	} catch(SocketException e) {
	    // shouldn't happen as we're bound
	}
	return -1;
    }

    /**
     * Returns the address of the endpoint this socket is connected to, or
     * <code>null</code> if it is unconnected.
     * @return a <code>SocketAddress</code> reprensenting the remote endpoint of this
     *	       socket, or <code>null</code> if it is not connected yet.
     * @see #getInetAddress()
     * @see #getPort()
     * @see #connect(SocketAddress, int)
     * @see #connect(SocketAddress)
     * @since 1.4
     */
    public SocketAddress getRemoteSocketAddress() {
	if (!isConnected())
	    return null;
	return new InetSocketAddress(getInetAddress(), getPort());
    }

    /**
     * Returns the address of the endpoint this socket is bound to, or
     * <code>null</code> if it is not bound yet.
     *
     * @return a <code>SocketAddress</code> representing the local endpoint of this
     *	       socket, or <code>null</code> if it is not bound yet.
     * @see #getLocalAddress()
     * @see #getLocalPort()
     * @see #bind(SocketAddress)
     * @since 1.4
     */

    public SocketAddress getLocalSocketAddress() {
	if (!isBound())
	    return null;
	return new InetSocketAddress(getLocalAddress(), getLocalPort());
    }

    /**
     * Returns the unique {@link java.nio.channels.SocketChannel SocketChannel}
     * object associated with this socket, if any.
     *
     * <p> A socket will have a channel if, and only if, the channel itself was
     * created via the {@link java.nio.channels.SocketChannel#open
     * SocketChannel.open} or {@link
     * java.nio.channels.ServerSocketChannel#accept ServerSocketChannel.accept}
     * methods.
     *
     * @return  the socket channel associated with this socket,
     *          or <tt>null</tt> if this socket was not created
     *          for a channel
     *
     * @since 1.4
     * @spec JSR-51
     */
    public SocketChannel getChannel() {
	return null;
    }

    /**
     * Returns an input stream for this socket.
     *
     * <p> If this socket has an associated channel then the resulting input
     * stream delegates all of its operations to the channel.  If the channel
     * is in non-blocking mode then the input stream's <tt>read</tt> operations
     * will throw an {@link java.nio.channels.IllegalBlockingModeException}.
     *
     * <p>Under abnormal conditions the underlying connection may be
     * broken by the remote host or the network software (for example
     * a connection reset in the case of TCP connections). When a
     * broken connection is detected by the network software the
     * following applies to the returned input stream :-
     *
     * <ul>
     *
     *   <li><p>The network software may discard bytes that are buffered
     *   by the socket. Bytes that aren't discarded by the network 
     *   software can be read using {@link java.io.InputStream#read read}.
     *
     *   <li><p>If there are no bytes buffered on the socket, or all
     *   buffered bytes have been consumed by  
     *   {@link java.io.InputStream#read read}, then all subsequent
     *   calls to {@link java.io.InputStream#read read} will throw an 
     *   {@link java.io.IOException IOException}. 
     *
     *   <li><p>If there are no bytes buffered on the socket, and the
     *   socket has not been closed using {@link #close close}, then
     *   {@link java.io.InputStream#available available} will
     *   return <code>0</code>.
     *
     * </ul>
     *
     * @return     an input stream for reading bytes from this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *             input stream, the socket is closed, the socket is
     *             not connected, or the socket input has been shutdown
     *             using {@link #shutdownInput()}
     *
     * @revised 1.4
     * @spec JSR-51
     */
    public InputStream getInputStream() throws IOException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	if (!isConnected())
	    throw new SocketException("Socket is not connected");
	if (isInputShutdown())
	    throw new SocketException("Socket input is shutdown");
	final Socket s = this;
	InputStream is = null;
	try {
	    is = (InputStream)
		AccessController.doPrivileged(new PrivilegedExceptionAction() {
		    public Object run() throws IOException {
			return impl.getInputStream();
		    }
		});
	} catch (java.security.PrivilegedActionException e) {
	    throw (IOException) e.getException();
	}
	return is;
    }

    /**
     * Returns an output stream for this socket.
     *
     * <p> If this socket has an associated channel then the resulting output
     * stream delegates all of its operations to the channel.  If the channel
     * is in non-blocking mode then the output stream's <tt>write</tt>
     * operations will throw an {@link
     * java.nio.channels.IllegalBlockingModeException}.
     *
     * @return     an output stream for writing bytes to this socket.
     * @exception  IOException  if an I/O error occurs when creating the
     *               output stream or if the socket is not connected.
     * @revised 1.4
     * @spec JSR-51
     */
    public OutputStream getOutputStream() throws IOException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	if (!isConnected())
	    throw new SocketException("Socket is not connected");
	if (isOutputShutdown())
	    throw new SocketException("Socket output is shutdown");
	final Socket s = this;
	OutputStream os = null;
	try {
	    os = (OutputStream)
		AccessController.doPrivileged(new PrivilegedExceptionAction() {
		    public Object run() throws IOException {
			return impl.getOutputStream();
		    }
		});
	} catch (java.security.PrivilegedActionException e) {
	    throw (IOException) e.getException();
	}
	return os;
    }

    /**
     * Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
     *
     * @param on <code>true</code> to enable TCP_NODELAY, 
     * <code>false</code> to disable.
     *
     * @exception SocketException if there is an error 
     * in the underlying protocol, such as a TCP error.
     * 
     * @since   JDK1.1
     *
     * @see #getTcpNoDelay()
     */
    public void setTcpNoDelay(boolean on) throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	getImpl().setOption(SocketOptions.TCP_NODELAY, new Boolean(on));
    }

    /**
     * Tests if TCP_NODELAY is enabled.
     *
     * @return a <code>boolean</code> indicating whether or not TCP_NODELAY is enabled.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @since   JDK1.1
     * @see #setTcpNoDelay(boolean)
     */
    public boolean getTcpNoDelay() throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	return ((Boolean) getImpl().getOption(SocketOptions.TCP_NODELAY)).booleanValue();
    }

    /**
     * Enable/disable SO_LINGER with the specified linger time in seconds. 
     * The maximum timeout value is platform specific.
     *
     * The setting only affects socket close.
     * 
     * @param on     whether or not to linger on.
     * @param linger how long to linger for, if on is true.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @exception IllegalArgumentException if the linger value is negative.
     * @since JDK1.1
     * @see #getSoLinger()
     */
    public void setSoLinger(boolean on, int linger) throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	if (!on) {
	    getImpl().setOption(SocketOptions.SO_LINGER, new Boolean(on));
	} else {
	    if (linger < 0) {
		throw new IllegalArgumentException("invalid value for SO_LINGER");
	    }
            if (linger > 65535)
                linger = 65535;
	    getImpl().setOption(SocketOptions.SO_LINGER, new Integer(linger));
	}
    }

    /**
     * Returns setting for SO_LINGER. -1 returns implies that the
     * option is disabled.
     *
     * The setting only affects socket close.
     *
     * @return the setting for SO_LINGER.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @since   JDK1.1
     * @see #setSoLinger(boolean, int)
     */
    public int getSoLinger() throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	Object o = getImpl().getOption(SocketOptions.SO_LINGER);
	if (o instanceof Integer) {
	    return ((Integer) o).intValue();
	} else {
	    return -1;
	}
    }

    /**
     * Send one byte of urgent data on the socket. The byte to be sent is the lowest eight
     * bits of the data parameter. The urgent byte is
     * sent after any preceding writes to the socket OutputStream
     * and before any future writes to the OutputStream.
     * @param data The byte of data to send
     * @exception IOException if there is an error
     *  sending the data.
     * @since 1.4
     */
    public void sendUrgentData (int data) throws IOException  {
        if (!getImpl().supportsUrgentData ()) {
            throw new SocketException ("Urgent data not supported");
        }
        getImpl().sendUrgentData (data);
    }

    /**
     * Enable/disable OOBINLINE (receipt of TCP urgent data)
     *
     * By default, this option is disabled and TCP urgent data received on a 
     * socket is silently discarded. If the user wishes to receive urgent data, then
     * this option must be enabled. When enabled, urgent data is received
     * inline with normal data. 
     * <p>
     * Note, only limited support is provided for handling incoming urgent 
     * data. In particular, no notification of incoming urgent data is provided 
     * and there is no capability to distinguish between normal data and urgent
     * data unless provided by a higher level protocol.
     *
     * @param on <code>true</code> to enable OOBINLINE, 
     * <code>false</code> to disable.
     *
     * @exception SocketException if there is an error 
     * in the underlying protocol, such as a TCP error.
     * 
     * @since   1.4
     *
     * @see #getOOBInline()
     */
    public void setOOBInline(boolean on) throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	getImpl().setOption(SocketOptions.SO_OOBINLINE, new Boolean(on));
    }

    /**
     * Tests if OOBINLINE is enabled.
     *
     * @return a <code>boolean</code> indicating whether or not OOBINLINE is enabled.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @since   1.4
     * @see #setOOBInline(boolean)
     */
    public boolean getOOBInline() throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	return ((Boolean) getImpl().getOption(SocketOptions.SO_OOBINLINE)).booleanValue();
    }

    /**
     *  Enable/disable SO_TIMEOUT with the specified timeout, in
     *  milliseconds.  With this option set to a non-zero timeout,
     *  a read() call on the InputStream associated with this Socket
     *  will block for only this amount of time.  If the timeout expires,
     *  a <B>java.net.SocketTimeoutException</B> is raised, though the
     *  Socket is still valid. The option <B>must</B> be enabled
     *  prior to entering the blocking operation to have effect. The
     *  timeout must be > 0.
     *  A timeout of zero is interpreted as an infinite timeout.
     * @param timeout the specified timeout, in milliseconds.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @since   JDK 1.1
     * @see #getSoTimeout()
     */
    public synchronized void setSoTimeout(int timeout) throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	if (timeout < 0)
	  throw new IllegalArgumentException("timeout can't be negative");

	getImpl().setOption(SocketOptions.SO_TIMEOUT, new Integer(timeout));
    }

    /**
     * Returns setting for SO_TIMEOUT.  0 returns implies that the
     * option is disabled (i.e., timeout of infinity).
     * @return the setting for SO_TIMEOUT
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @since   JDK1.1
     * @see #setSoTimeout(int)
     */
    public synchronized int getSoTimeout() throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	Object o = getImpl().getOption(SocketOptions.SO_TIMEOUT);
	/* extra type safety */
	if (o instanceof Integer) {
	    return ((Integer) o).intValue();
	} else {
	    return 0;
	}
    }

    /**
     * Sets the SO_SNDBUF option to the specified value for this
     * <tt>Socket</tt>. The SO_SNDBUF option is used by the platform's
     * networking code as a hint for the size to set
     * the underlying network I/O buffers.
     *
     * <p>Because SO_SNDBUF is a hint, applications that want to
     * verify what size the buffers were set to should call
     * {@link #getSendBufferSize()}.
     *
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     *
     * @param size the size to which to set the send buffer
     * size. This value must be greater than 0.
     *
     * @exception IllegalArgumentException if the 
     * value is 0 or is negative.
     *
     * @see #getSendBufferSize()
     * @since 1.2
     */
    public synchronized void setSendBufferSize(int size)
    throws SocketException{
	if (!(size > 0)) {
	    throw new IllegalArgumentException("negative send size");
	}
	if (isClosed())
	    throw new SocketException("Socket is closed");
	getImpl().setOption(SocketOptions.SO_SNDBUF, new Integer(size));
    }

    /**
     * Get value of the SO_SNDBUF option for this <tt>Socket</tt>, 
     * that is the buffer size used by the platform 
     * for output on this <tt>Socket</tt>.
     * @return the value of the SO_SNDBUF option for this <tt>Socket</tt>.
     *
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     *
     * @see #setSendBufferSize(int)
     * @since 1.2
     */
    public synchronized int getSendBufferSize() throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	int result = 0;
	Object o = getImpl().getOption(SocketOptions.SO_SNDBUF);
	if (o instanceof Integer) {
	    result = ((Integer)o).intValue();
	}
	return result;
    }

    /**
     * Sets the SO_RCVBUF option to the specified value for this
     * <tt>Socket</tt>. The SO_RCVBUF option is used by the platform's
     * networking code as a hint for the size to set
     * the underlying network I/O buffers.
     *
     * <p>Increasing the receive buffer size can increase the performance of
     * network I/O for high-volume connection, while decreasing it can
     * help reduce the backlog of incoming data. 
     *
     * <p>Because SO_RCVBUF is a hint, applications that want to
     * verify what size the buffers were set to should call
     * {@link #getReceiveBufferSize()}.
     *
     * <p>The value of SO_RCVBUF is also used to set the TCP receive window
     * that is advertized to the remote peer. Generally, the window size
     * can be modified at any time when a socket is connected. However, if
     * a receive window larger than 64K is required then this must be requested
     * <B>before</B> the socket is connected to the remote peer. There are two
     * cases to be aware of:<p>
     * <ol>
     * <li>For sockets accepted from a ServerSocket, this must be done by calling
     * {@link ServerSocket#setReceiveBufferSize(int)} before the ServerSocket 
     * is bound to a local address.<p></li>
     * <li>For client sockets, setReceiveBufferSize() must be called before
     * connecting the socket to its remote peer.<p></li></ol>
     * @param size the size to which to set the receive buffer
     * size. This value must be greater than 0.
     *
     * @exception IllegalArgumentException if the value is 0 or is
     * negative.
     *
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error.
     * 
     * @see #getReceiveBufferSize()
     * @see ServerSocket#setReceiveBufferSize(int)
     * @since 1.2
     */
    public synchronized void setReceiveBufferSize(int size)
    throws SocketException{
	if (size <= 0) {
	    throw new IllegalArgumentException("invalid receive size");
	}
	if (isClosed())
	    throw new SocketException("Socket is closed");
	getImpl().setOption(SocketOptions.SO_RCVBUF, new Integer(size));
    }

    /**
     * Gets the value of the SO_RCVBUF option for this <tt>Socket</tt>, 
     * that is the buffer size used by the platform for 
     * input on this <tt>Socket</tt>.
     *
     * @return the value of the SO_RCVBUF option for this <tt>Socket</tt>.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @see #setReceiveBufferSize(int)
     * @since 1.2
     */
    public synchronized int getReceiveBufferSize()
    throws SocketException{
	if (isClosed())
	    throw new SocketException("Socket is closed");
	int result = 0;
	Object o = getImpl().getOption(SocketOptions.SO_RCVBUF);
	if (o instanceof Integer) {
	    result = ((Integer)o).intValue();
	}
	return result;
    }

    /**
     * Enable/disable SO_KEEPALIVE.
     * 
     * @param on     whether or not to have socket keep alive turned on.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @since 1.3 
     * @see #getKeepAlive()
     */
    public void setKeepAlive(boolean on) throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
        getImpl().setOption(SocketOptions.SO_KEEPALIVE, new Boolean(on));
    }

    /**
     * Tests if SO_KEEPALIVE is enabled.
     *
     * @return a <code>boolean</code> indicating whether or not SO_KEEPALIVE is enabled.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @since   1.3
     * @see #setKeepAlive(boolean)
     */
    public boolean getKeepAlive() throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	return ((Boolean) getImpl().getOption(SocketOptions.SO_KEEPALIVE)).booleanValue();
    }

    /**
     * Sets traffic class or type-of-service octet in the IP
     * header for packets sent from this Socket.
     * As the underlying network implementation may ignore this
     * value applications should consider it a hint.
     *
     * <P> The tc <B>must</B> be in the range <code> 0 <= tc <=
     * 255</code> or an IllegalArgumentException will be thrown.
     * <p>Notes:
     * <p> for Internet Protocol v4 the value consists of an octet
     * with precedence and TOS fields as detailed in RFC 1349. The
     * TOS field is bitset created by bitwise-or'ing values such
     * the following :-
     * <p>
     * <UL>
     * <LI><CODE>IPTOS_LOWCOST (0x02)</CODE></LI>
     * <LI><CODE>IPTOS_RELIABILITY (0x04)</CODE></LI>
     * <LI><CODE>IPTOS_THROUGHPUT (0x08)</CODE></LI>
     * <LI><CODE>IPTOS_LOWDELAY (0x10)</CODE></LI>
     * </UL>
     * The last low order bit is always ignored as this
     * corresponds to the MBZ (must be zero) bit.
     * <p>
     * Setting bits in the precedence field may result in a
     * SocketException indicating that the operation is not
     * permitted.
     * <p>
     * for Internet Protocol v6 <code>tc</code> is the value that
     * would be placed into the sin6_flowinfo field of the IP header.
     *
     * @param tc        an <code>int</code> value for the bitset.
     * @throws SocketException if there is an error setting the
     * traffic class or type-of-service
     * @since 1.4
     * @see #getTrafficClass
     */
    public void setTrafficClass(int tc) throws SocketException {
	if (tc < 0 || tc > 255)
	    throw new IllegalArgumentException("tc is not in range 0 -- 255");

	if (isClosed())
	    throw new SocketException("Socket is closed");
        getImpl().setOption(SocketOptions.IP_TOS, new Integer(tc));
    }

    /**
     * Gets traffic class or type-of-service in the IP header
     * for packets sent from this Socket
     * <p>
     * As the underlying network implementation may ignore the
     * traffic class or type-of-service set using {@link #setTrafficClass()}
     * this method may return a different value than was previously
     * set using the {@link #setTrafficClass()} method on this Socket.
     *
     * @return the traffic class or type-of-service already set
     * @throws SocketException if there is an error obtaining the
     * traffic class or type-of-service value.
     * @since 1.4
     * @see #setTrafficClass
     */
    public int getTrafficClass() throws SocketException {
        return ((Integer) (getImpl().getOption(SocketOptions.IP_TOS))).intValue();
    }

    /**
     * Enable/disable the SO_REUSEADDR socket option.
     * <p>
     * When a TCP connection is closed the connection may remain
     * in a timeout state for a period of time after the connection
     * is closed (typically known as the <tt>TIME_WAIT</tt> state
     * or <tt>2MSL</tt> wait state).
     * For applications using a well known socket address or port 
     * it may not be possible to bind a socket to the required
     * <tt>SocketAddress</tt> if there is a connection in the
     * timeout state involving the socket address or port.
     * <p>
     * Enabling <tt>SO_REUSEADDR</tt> prior to binding the socket
     * using {@link #bind(SocketAddress)} allows the socket to be
     * bound even though a previous connection is in a timeout
     * state.
     * <p>
     * When a <tt>Socket</tt> is created the initial setting
     * of <tt>SO_REUSEADDR</tt> is disabled.
     * <p>
     * The behaviour when <tt>SO_REUSEADDR</tt> is enabled or
     * disabled after a socket is bound (See {@link #isBound()})
     * is not defined.
     * 
     * @param on  whether to enable or disable the socket option
     * @exception SocketException if an error occurs enabling or
     *            disabling the <tt>SO_RESUEADDR</tt> socket option,
     *		  or the socket is closed.
     * @since 1.4
     * @see #getReuseAddress()     
     * @see #bind(SocketAddress)
     * @see #isClosed()
     * @see #isBound()
     */
    public void setReuseAddress(boolean on) throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
        getImpl().setOption(SocketOptions.SO_REUSEADDR, new Boolean(on));
    }

    /**
     * Tests if SO_REUSEADDR is enabled.
     *
     * @return a <code>boolean</code> indicating whether or not SO_REUSEADDR is enabled.
     * @exception SocketException if there is an error
     * in the underlying protocol, such as a TCP error. 
     * @since   1.4
     * @see #setReuseAddress(boolean)
     */
    public boolean getReuseAddress() throws SocketException {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	return ((Boolean) (getImpl().getOption(SocketOptions.SO_REUSEADDR))).booleanValue();
    }

    /**
     * Closes this socket.
     * <p>
     * Any thread currently blocked in an I/O operation upon this socket
     * will throw a {@link SocketException}.
     * <p>
     * Once a socket has been closed, it is not available for further networking
     * use (i.e. can't be reconnected or rebound). A new socket needs to be
     * created.
     *
     * <p> If this socket has an associated channel then the channel is closed
     * as well.
     *
     * @exception  IOException  if an I/O error occurs when closing this socket.
     * @revised 1.4
     * @spec JSR-51
     * @see #isClosed
     */
    public synchronized void close() throws IOException {
	synchronized(closeLock) {
	    if (isClosed())
		return;
	    if (created)
		impl.close();
	    closed = true;
	}
    }

    /**
     * Places the input stream for this socket at "end of stream".
     * Any data sent to the input stream side of the socket is acknowledged
     * and then silently discarded.
     * <p>
     * If you read from a socket input stream after invoking 
     * shutdownInput() on the socket, the stream will return EOF.
     *
     * @exception IOException if an I/O error occurs when shutting down this
     * socket.
     *
     * @since 1.3
     * @see java.net.Socket#shutdownOutput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @see #isInputShutdown
     */
    public void shutdownInput() throws IOException
    {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	if (!isConnected())
	    throw new SocketException("Socket is not connected");
	if (isInputShutdown())
	    throw new SocketException("Socket input is already shutdown");
	getImpl().shutdownInput();
	shutIn = true;
    }
    
    /**
     * Disables the output stream for this socket.
     * For a TCP socket, any previously written data will be sent
     * followed by TCP's normal connection termination sequence.
     *
     * If you write to a socket output stream after invoking 
     * shutdownOutput() on the socket, the stream will throw 
     * an IOException.
     *
     * @exception IOException if an I/O error occurs when shutting down this
     * socket.
     *
     * @since 1.3
     * @see java.net.Socket#shutdownInput()
     * @see java.net.Socket#close()
     * @see java.net.Socket#setSoLinger(boolean, int)
     * @see #isOutputShutdown
     */
    public void shutdownOutput() throws IOException
    {
	if (isClosed())
	    throw new SocketException("Socket is closed");
	if (!isConnected())
	    throw new SocketException("Socket is not connected");
	if (isOutputShutdown())
	    throw new SocketException("Socket output is already shutdown");
	getImpl().shutdownOutput();
	shutOut = true;
    }

    /**
     * Converts this socket to a <code>String</code>.
     *
     * @return  a string representation of this socket.
     */
    public String toString() {
	try {
	    if (isConnected())
		return "Socket[addr=" + getImpl().getInetAddress() +
		    ",port=" + getImpl().getPort() +
		    ",localport=" + getImpl().getLocalPort() + "]";
	} catch (SocketException e) {
	}
	return "Socket[unconnected]";
    }

    /**
     * Returns the connection state of the socket.
     *
     * @return true if the socket successfuly connected to a server
     * @since 1.4
     */
    public boolean isConnected() {
	// Before 1.3 Sockets were always connected during creation
	return connected || oldImpl;
    }

    /**
     * Returns the binding state of the socket.
     *
     * @return true if the socket successfuly bound to an address
     * @since 1.4
     * @see #bind
     */
    public boolean isBound() {
	// Before 1.3 Sockets were always bound during creation
	return bound || oldImpl;
    }

    /**
     * Returns the closed state of the socket.
     *
     * @return true if the socket has been closed
     * @since 1.4
     * @see #close
     */
    public boolean isClosed() {
	synchronized(closeLock) {
	    return closed;
	}
    }

    /**
     * Returns wether the read-half of the socket connection is closed.
     *
     * @return true if the input of the socket has been shutdown
     * @since 1.4
     * @see #shutdownInput
     */
    public boolean isInputShutdown() {
	return shutIn;
    }

    /**
     * Returns wether the write-half of the socket connection is closed.
     *
     * @return true if the output of the socket has been shutdown
     * @since 1.4
     * @see #shutdownOutput
     */
    public boolean isOutputShutdown() {
	return shutOut;
    }

    /**
     * The factory for all client sockets.
     */
    private static SocketImplFactory factory = null;
    private static synchronized void checkSocks() {
	int port = -1;
	String socksPort = null;
	String useSocks = null;

	if (factory == null) {
	    
	    useSocks = (String) AccessController.doPrivileged(
		   new sun.security.action.GetPropertyAction("socksProxyHost"));
	    if (useSocks == null || useSocks.length() <= 0)
		return;

	    socksPort = (String) AccessController.doPrivileged(
		       new sun.security.action.GetPropertyAction("socksProxyPort"));
	    if (socksPort != null && socksPort.length() > 0) {
		try {
		    port = Integer.parseInt(socksPort);
		} catch (Exception e) {
		    port = -1;
		}
	    }
	    if (useSocks != null)
		factory = new SocksSocketImplFactory(useSocks, port);
	} else if (factory instanceof SocksSocketImplFactory) {
	    useSocks = (String) AccessController.doPrivileged(
		   new sun.security.action.GetPropertyAction("socksProxyHost"));
	    if (useSocks == null || useSocks.length() <= 0)
		factory = null;
	}
    }

    /**
     * Sets the client socket implementation factory for the
     * application. The factory can be specified only once.
     * <p>
     * When an application creates a new client socket, the socket
     * implementation factory's <code>createSocketImpl</code> method is
     * called to create the actual socket implementation.
     * 
     * <p>If there is a security manager, this method first calls
     * the security manager's <code>checkSetFactory</code> method 
     * to ensure the operation is allowed. 
     * This could result in a SecurityException.
     *
     * @param      fac   the desired factory.
     * @exception  IOException  if an I/O error occurs when setting the
     *               socket factory.
     * @exception  SocketException  if the factory is already defined.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkSetFactory</code> method doesn't allow the operation.
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        SecurityManager#checkSetFactory
     */
    public static synchronized void setSocketImplFactory(SocketImplFactory fac)
	throws IOException
    {
	if (factory != null) {
	    throw new SocketException("factory already defined");
	}
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
	    security.checkSetFactory();
	}
	factory = fac;
    }
}

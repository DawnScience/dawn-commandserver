'''
Shared py/jython functions which for use in consumer interpreter instances
'''

#Borrowed from http://blog.datasingularity.com/?p=134
#N.b. if we add methods starting '_' they will not be nuked.
def __dlsCleanup():
	"""Clear's all non-standard global values from namespace"""
	for uniquevar in [var for var in globals().copy() if var[0] != "_" and var != '__dlsCleanup']:
		del globals()[uniquevar]
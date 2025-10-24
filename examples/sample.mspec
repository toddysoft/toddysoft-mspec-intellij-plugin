// Sample MSpec file demonstrating the language syntax
// This is a simplified example based on Apache PLC4X protocol specifications

[constants
    [const uint 16 PROTOCOL_VERSION 1]
    [const uint 8 MAX_PDU_SIZE 240]
]

[type Message
    [simple uint 16 length]
    [simple uint 8 messageType]
    [typeSwitch messageType
        ['0x01' ReadRequest
            [simple uint 16 numberOfItems]
            [array Item items count 'numberOfItems']
        ]
        ['0x02' ReadResponse
            [simple uint 16 numberOfItems]
            [array ResponseItem items count 'numberOfItems']
        ]
        ['0x03' WriteRequest
            [simple uint 16 numberOfItems]
            [array WriteItem items count 'numberOfItems']
        ]
    ]
]

[type Item
    [simple uint 8 itemType]
    [simple uint 16 itemLength]
    [array byte itemData count 'itemLength']
]

[type ResponseItem
    [simple uint 8 returnCode]
    [simple uint 16 dataLength]
    [array byte data count 'dataLength']
]

[type WriteItem
    [simple uint 8 itemType]
    [simple uint 16 valueLength]
    [array byte value count 'valueLength']
]

[enum uint 8 ErrorCode
    ['0x00' NO_ERROR]
    ['0x01' INVALID_REQUEST]
    ['0x02' ACCESS_DENIED]
    ['0x03' INVALID_DATA]
]
